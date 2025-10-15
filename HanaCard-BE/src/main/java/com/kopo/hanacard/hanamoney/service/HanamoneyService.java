package com.kopo.hanacard.hanamoney.service;

import com.kopo.hanacard.common.exception.BusinessException;
import com.kopo.hanacard.common.exception.ErrorCode;
import com.kopo.hanacard.hanamoney.domain.HanamoneyMembership;
import com.kopo.hanacard.hanamoney.domain.HanamoneyTransaction;
import com.kopo.hanacard.hanamoney.repository.HanamoneyMembershipRepository;
import com.kopo.hanacard.hanamoney.repository.HanamoneyTransactionRepository;
import com.kopo.hanacard.user.domain.User;
import com.kopo.hanacard.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HanamoneyService {

    private final HanamoneyMembershipRepository hanamoneyMembershipRepository;
    private final HanamoneyTransactionRepository hanamoneyTransactionRepository;
    private final UserService userService;
    private final HanaGreenWorldIntegrationService hanaGreenWorldIntegrationService;

    @Transactional
    public HanamoneyMembership createHanamoneyMembership(Long userId) {
        User user = userService.getUserById(userId);

        // 이미 멤버십이 있는지 확인
        if (hanamoneyMembershipRepository.findByUser_Id(userId).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS, "이미 하나머니 멤버십이 존재합니다.");
        }

        HanamoneyMembership membership = HanamoneyMembership.builder()
                .user(user)
                .membershipId(UUID.randomUUID().toString())
                .build();

        return hanamoneyMembershipRepository.save(membership);
    }

    @Transactional
    public HanamoneyMembership getHanamoneyMembershipByUserId(Long userId) {
        // 하나카드 DB에서 직접 조회
        return hanamoneyMembershipRepository.findByUser_Id(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "하나머니 멤버십을 찾을 수 없습니다."));
    }

    @Transactional
    public HanamoneyMembership getHanamoneyMembershipById(Long membershipId) {
        return hanamoneyMembershipRepository.findById(membershipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "하나머니 멤버십을 찾을 수 없습니다."));
    }

    @Transactional
    public HanamoneyMembership earn(Long userId, Long amount, String description) {
        HanamoneyMembership membership = getHanamoneyMembershipByUserId(userId);
        
        membership.earn(amount);
        hanamoneyMembershipRepository.save(membership);

        // 거래 내역 생성
        createTransaction(membership, amount, HanamoneyTransaction.TransactionType.EARN, description);
        
        // 하나그린세상에 동기화
        hanaGreenWorldIntegrationService.syncToGreenWorld(userId, amount, "EARN", description);
        
        return membership;
    }

    @Transactional
    public HanamoneyMembership spend(Long userId, Long amount, String description) {
        HanamoneyMembership membership = getHanamoneyMembershipByUserId(userId);
        
        if (membership.getBalance() < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "잔액이 부족합니다.");
        }
        
        membership.spend(amount);
        hanamoneyMembershipRepository.save(membership);

        // 거래 내역 생성
        createTransaction(membership, amount, HanamoneyTransaction.TransactionType.SPEND, description);
        
        // 하나그린세상에 동기화
        hanaGreenWorldIntegrationService.syncToGreenWorld(userId, amount, "SPEND", description);
        
        return membership;
    }

    @Transactional
    public HanamoneyMembership transferTo(Long fromUserId, Long toUserId, Long amount, String description) {
        HanamoneyMembership fromMembership = getHanamoneyMembershipByUserId(fromUserId);
        HanamoneyMembership toMembership = getHanamoneyMembershipByUserId(toUserId);
        
        if (fromMembership.getBalance() < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "잔액이 부족합니다.");
        }
        
        fromMembership.spend(amount);
        toMembership.earn(amount);
        
        hanamoneyMembershipRepository.save(fromMembership);
        hanamoneyMembershipRepository.save(toMembership);

        // 거래 내역 생성
        createTransaction(fromMembership, amount, HanamoneyTransaction.TransactionType.TRANSFER_OUT, description);
        createTransaction(toMembership, amount, HanamoneyTransaction.TransactionType.TRANSFER_IN, description);
        
        return fromMembership;
    }

    @Transactional
    public HanamoneyMembership atmWithdraw(Long userId, Long amount, String description) {
        HanamoneyMembership membership = getHanamoneyMembershipByUserId(userId);
        
        if (membership.getBalance() < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "잔액이 부족합니다.");
        }
        
        membership.spend(amount);
        hanamoneyMembershipRepository.save(membership);

        // 거래 내역 생성
        createTransaction(membership, amount, HanamoneyTransaction.TransactionType.ATM_WITHDRAWAL, description);
        
        return membership;
    }

    @Transactional
    public HanamoneyMembership exchangeToPartner(Long userId, Long amount, String partnerName, String description) {
        HanamoneyMembership membership = getHanamoneyMembershipByUserId(userId);
        
        if (membership.getBalance() < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "잔액이 부족합니다.");
        }
        
        membership.spend(amount);
        hanamoneyMembershipRepository.save(membership);

        // 거래 내역 생성
        String transactionDescription = String.format("%s - %s", partnerName, description);
        createTransaction(membership, amount, HanamoneyTransaction.TransactionType.PARTNER_EXCHANGE, transactionDescription);
        
        return membership;
    }

    public List<HanamoneyTransaction> getTransactionHistory(Long userId) {
        HanamoneyMembership membership = getHanamoneyMembershipByUserId(userId);
        return hanamoneyTransactionRepository.findByMembership(membership);
    }

    public List<HanamoneyTransaction> getTransactionHistoryByType(Long userId, HanamoneyTransaction.TransactionType transactionType) {
        HanamoneyMembership membership = getHanamoneyMembershipByUserId(userId);
        return hanamoneyTransactionRepository.findByMembershipAndTransactionType(membership, transactionType);
    }

    public List<HanamoneyTransaction> getTransactionHistoryByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        HanamoneyMembership membership = getHanamoneyMembershipByUserId(userId);
        return hanamoneyTransactionRepository.findByMembershipAndCreatedAtBetween(membership, startDate, endDate);
    }

    private void createTransaction(HanamoneyMembership membership, Long amount, 
                                 HanamoneyTransaction.TransactionType transactionType, String description) {
        HanamoneyTransaction transaction = HanamoneyTransaction.builder()
                .membership(membership)
                .amount(amount)
                .balanceAfter(membership.getBalance())
                .transactionType(transactionType)
                .description(description)
                .build();
        
        hanamoneyTransactionRepository.save(transaction);
    }
}