package com.kopo.hanacard.card.service;

import com.kopo.hanacard.card.domain.CardTransaction;
import com.kopo.hanacard.card.domain.UserCard;
import com.kopo.hanacard.card.dto.CardConsumptionSummaryResponse;
import com.kopo.hanacard.card.dto.CardTransactionResponse;
import com.kopo.hanacard.card.repository.CardTransactionRepository;
import com.kopo.hanacard.card.repository.UserCardRepository;
import com.kopo.hanacard.common.exception.BusinessException;
import com.kopo.hanacard.common.exception.ErrorCode;
import com.kopo.hanacard.user.domain.User;
import com.kopo.hanacard.user.repository.UserRepository;
import com.kopo.hanacard.user.service.UserService;
import com.kopo.hanacard.card.event.CardTransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CardTransactionService {

    private final CardTransactionRepository cardTransactionRepository;
    private final UserCardRepository userCardRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final WebhookService webhookService;

    public List<CardTransactionResponse> getUserCardTransactions(Long userId) {
        List<UserCard> userCards = userCardRepository.findByUserIdAndIsActive(userId, true);
        
        if (userCards.isEmpty()) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
        }

        // 첫 번째 활성 카드의 거래내역 조회
        UserCard primaryCard = userCards.get(0);
        List<CardTransaction> transactions = cardTransactionRepository.findByUserCardOrderByTransactionDateDesc(primaryCard);
        
        return transactions.stream()
                .map(CardTransactionResponse::new)
                .collect(Collectors.toList());
    }

    public CardConsumptionSummaryResponse getMonthlyConsumptionSummary(Long userId) {
        log.info("월간 소비현황 조회 시작 - 사용자ID: {}", userId);
        
        if (userId == null) {
            log.warn("사용자 ID가 null입니다.");
            return createEmptyConsumptionSummary();
        }
        
        List<UserCard> userCards = userCardRepository.findByUserIdAndIsActive(userId, true);
        
        if (userCards.isEmpty()) {
            log.warn("활성 카드를 찾을 수 없습니다 - 사용자ID: {}", userId);
            return createEmptyConsumptionSummary();
        }

        UserCard primaryCard = userCards.get(0);
        log.info("주 카드 조회 완료 - 카드ID: {}", primaryCard.getId());
        
        // 이번 달 첫날부터 현재까지
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        
        // 이번 달 거래내역 조회
        List<CardTransaction> monthlyTransactions = cardTransactionRepository
                .findByUserCardAndTransactionDateBetweenOrderByTransactionDateDesc(
                        primaryCard, startOfMonth, LocalDateTime.now());

        // 총 소비금액 계산
        Long totalAmount = monthlyTransactions.stream()
                .mapToLong(transaction -> transaction.getAmount() != null ? transaction.getAmount() : 0L)
                .sum();

        // 총 캐시백 계산
        Long totalCashback = monthlyTransactions.stream()
                .mapToLong(transaction -> transaction.getCashbackAmount() != null ? transaction.getCashbackAmount() : 0L)
                .sum();

        // 카테고리별 소비금액 계산
        Map<String, Long> categoryAmounts = monthlyTransactions.stream()
                .collect(Collectors.groupingBy(
                        CardTransaction::getCategory,
                        Collectors.summingLong(CardTransaction::getAmount)
                ));

        // 최근 거래내역 (최대 10건)
        List<CardTransactionResponse> recentTransactions = monthlyTransactions.stream()
                .limit(10)
                .map(CardTransactionResponse::new)
                .collect(Collectors.toList());

        return new CardConsumptionSummaryResponse(totalAmount, totalCashback, categoryAmounts, recentTransactions);
    }

    private CardConsumptionSummaryResponse createEmptyConsumptionSummary() {
        return new CardConsumptionSummaryResponse(
            0L, 
            0L, 
            new HashMap<>(), 
            new ArrayList<>()
        );
    }


    public List<CardTransactionResponse> getTransactionsByCategory(Long userId, String category) {
        List<UserCard> userCards = userCardRepository.findByUserIdAndIsActive(userId, true);
        
        if (userCards.isEmpty()) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
        }

        UserCard primaryCard = userCards.get(0);
        List<CardTransaction> transactions = cardTransactionRepository.findByUserCardOrderByTransactionDateDesc(primaryCard);
        
        return transactions.stream()
                .filter(transaction -> transaction.getCategory().equals(category))
                .map(CardTransactionResponse::new)
                .collect(Collectors.toList());
    }

    public CardConsumptionSummaryResponse getEcoConsumptionAnalysis(Long userId) {
        List<UserCard> userCards = userCardRepository.findByUserIdAndIsActiveTrue(userId);
        if (userCards.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_CARD_NOT_FOUND);
        }

        UserCard userCard = userCards.get(0);
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        List<CardTransaction> transactions = cardTransactionRepository
                .findByUserCardAndTransactionDateBetween(userCard, startOfMonth, endOfMonth);

        // 친환경 태그들
        List<String> ecoTags = List.of("친환경", "전기차", "대중교통", "공유킥보드", "리필샵", "유기농", "재활용");

        Map<String, Long> categoryAmounts = new HashMap<>();
        Map<String, Long> ecoCategoryAmounts = new HashMap<>();
        long totalAmount = 0;
        long ecoAmount = 0;
        long totalCashback = 0;
        long ecoCashback = 0;

        for (CardTransaction transaction : transactions) {
            String category = transaction.getCategory();
            Long amount = transaction.getAmount();
            Long cashback = transaction.getCashbackAmount();

            totalAmount += amount;
            totalCashback += cashback;

            categoryAmounts.merge(category, amount, Long::sum);

            // 친환경 태그가 있는지 확인
            boolean isEco = false;
            if (transaction.getTags() != null) {
                for (String tag : ecoTags) {
                    if (transaction.getTags().contains(tag)) {
                        isEco = true;
                        break;
                    }
                }
            }

            if (isEco || ecoTags.contains(category)) {
                ecoAmount += amount;
                ecoCashback += cashback;
                ecoCategoryAmounts.merge(category, amount, Long::sum);
            }
        }

        double ecoRatio = totalAmount > 0 ? (double) ecoAmount / totalAmount * 100 : 0;

        return CardConsumptionSummaryResponse.builder()
                .totalAmount(totalAmount)
                .totalCashback(totalCashback)
                .ecoAmount(ecoAmount)
                .ecoCashback(ecoCashback)
                .ecoRatio(ecoRatio)
                .categoryAmounts(categoryAmounts)
                .ecoCategoryAmounts(ecoCategoryAmounts)
                .build();
    }

    public List<CardTransactionResponse> getTransactionsByTag(Long userId, String tag) {
        List<UserCard> userCards = userCardRepository.findByUserIdAndIsActiveTrue(userId);
        if (userCards.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_CARD_NOT_FOUND);
        }

        UserCard userCard = userCards.get(0);
        List<CardTransaction> transactions = cardTransactionRepository.findByUserCard(userCard);

        return transactions.stream()
                .filter(transaction -> transaction.getTags() != null && 
                        transaction.getTags().contains(tag))
                .map(CardTransactionResponse::new)
                .collect(Collectors.toList());
    }

    public List<CardTransactionResponse> getCardTransactionsByPhone(String phoneNumber) {
        User user = userService.getUserByPhoneNumber(phoneNumber);
        List<UserCard> userCards = userCardRepository.findByUserAndIsActiveTrue(user);
        
        if (userCards.isEmpty()) {
            return List.of();
        }

        // 첫 번째 활성 카드의 거래내역 조회
        UserCard primaryCard = userCards.get(0);
        List<CardTransaction> transactions = cardTransactionRepository.findByUserCardOrderByTransactionDateDesc(primaryCard);
        
        return transactions.stream()
                .map(CardTransactionResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public CardTransaction createCardTransaction(Long userId, String merchantName, String businessNumber,
                                               Long amount, String category, String merchantCategory) {
        try {
            // 1. 사용자 카드 조회 (주 카드 사용)
            List<UserCard> userCards = userCardRepository.findByUserIdAndIsActive(userId, true);
            if (userCards.isEmpty()) {
                throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
            }
            
            UserCard primaryCard = userCards.get(0); // 첫 번째 카드를 주 카드로 사용
            
            // 2. 카드 거래 생성
            CardTransaction transaction = CardTransaction.builder()
                    .userCard(primaryCard)
                    .merchantName(merchantName)
                    .businessNumber(businessNumber)
                    .amount(amount)
                    .category(category)
                    .merchantCategory(merchantCategory)
                    .transactionDate(LocalDateTime.now())
                    .description(String.format("%s에서 %d원 결제", merchantName, amount))
                    .cashbackAmount(0L) // 기본값
                    .cashbackRate(java.math.BigDecimal.ZERO) // 기본값
                    .tags("") // 기본값
                    .build();
            
            // 3. 거래 저장
            CardTransaction savedTransaction = cardTransactionRepository.save(transaction);
            
            // 4. 이벤트 발행 (사업자 번호가 있는 경우만)
            if (businessNumber != null && !businessNumber.trim().isEmpty()) {
                CardTransactionCreatedEvent event = CardTransactionCreatedEvent.of(
                    savedTransaction.getId(),
                    userId,
                    merchantName,
                    businessNumber,
                    amount,
                    savedTransaction.getTransactionDate(),
                    category,
                    merchantCategory
                );
                
                eventPublisher.publishEvent(event);
                
                // 5. 하나그린세상에 웹훅 전송
                webhookService.sendCardTransactionWebhook(savedTransaction);
                
                log.info("카드 거래 생성, 이벤트 발행 및 웹훅 전송 완료 - 거래ID: {}, 사용자ID: {}, 가맹점: {}, 사업자번호: {}",
                        savedTransaction.getId(), userId, merchantName, businessNumber);
            } else {
                log.info("카드 거래 생성 완료 (사업자번호 없음) - 거래ID: {}, 사용자ID: {}, 가맹점: {}",
                        savedTransaction.getId(), userId, merchantName);
            }
            
            return savedTransaction;
            
        } catch (Exception e) {
            log.error("카드 거래 생성 실패 - 사용자ID: {}, 가맹점: {}, 에러: {}", userId, merchantName, e.getMessage(), e);
            throw new BusinessException(ErrorCode.TRANSACTION_CREATE_FAILED);
        }
    }
}

