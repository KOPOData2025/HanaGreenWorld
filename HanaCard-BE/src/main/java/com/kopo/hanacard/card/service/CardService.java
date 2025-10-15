package com.kopo.hanacard.card.service;

import com.kopo.hanacard.common.exception.BusinessException;
import com.kopo.hanacard.common.exception.ErrorCode;
import com.kopo.hanacard.card.domain.CardProduct;
import com.kopo.hanacard.card.domain.CardBenefit;
import com.kopo.hanacard.card.domain.UserCard;
import com.kopo.hanacard.card.dto.UserCardResponse;
import com.kopo.hanacard.card.repository.CardBenefitRepository;
import com.kopo.hanacard.card.repository.CardProductRepository;
import com.kopo.hanacard.card.repository.UserCardRepository;
import com.kopo.hanacard.user.domain.User;
import com.kopo.hanacard.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

    private final CardProductRepository cardProductRepository;
    private final UserCardRepository userCardRepository;
    private final CardBenefitRepository cardBenefitRepository;
    private final UserService userService;

    public List<CardProduct> getAllCards() {
        return cardProductRepository.findByIsActiveTrue();
    }

    public CardProduct getCardById(Long id) {
        return cardProductRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));
    }

    @Transactional
    public UserCard registerCard(Long userId, Long cardId) {
        User user = userService.getUserById(userId);
        CardProduct cardProduct = getCardById(cardId);

        // 카드번호 생성
        String cardNumber = generateCardNumber();
        String cardNumberMasked = maskCardNumber(cardNumber);
        
        // 만료일 설정 (5년 후)
        LocalDate expiryDate = LocalDate.now().plusYears(5);
        
        // CVV 생성
        String cvv = generateCVV();

        UserCard userCard = UserCard.builder()
                .user(user)
                .cardProduct(cardProduct)
                .cardNumber(cardNumber)
                .cardNumberMasked(cardNumberMasked)
                .expiryDate(expiryDate)
                .cvv(cvv)
                .isActive(true)
                .build();

        return userCardRepository.save(userCard);
    }

    public List<UserCardResponse> getUserCardResponses(Long userId) {
        List<UserCard> userCards = userCardRepository.findByUserAndIsActiveTrue(userService.getUserById(userId));
        return userCards.stream()
                .map(UserCardResponse::new)
                .toList();
    }

    public UserCard getUserCardByNumber(String cardNumber) {
        return userCardRepository.findByCardNumber(cardNumber)
                .filter(userCard -> userCard.getIsActive())
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));
    }

    @Transactional
    public void deactivateCard(String cardNumber) {
        UserCard userCard = getUserCardByNumber(cardNumber);
        userCard.deactivate();
        userCardRepository.save(userCard);
    }

    public List<CardBenefit> getCardBenefits(Long cardId) {
        CardProduct cardProduct = getCardById(cardId);
        return cardBenefitRepository.findByCardProductAndIsActiveTrue(cardProduct);
    }

    public List<CardBenefit> getUserCardBenefits(Long userId) {
        User user = userService.getUserById(userId);
        List<UserCard> userCards = userCardRepository.findByUserAndIsActiveTrue(user);
        
        return userCards.stream()
                .flatMap(userCard -> cardBenefitRepository.findByCardProductAndIsActiveTrue(userCard.getCardProduct()).stream())
                .toList();
    }

    private String generateCardNumber() {
        return "4000-" + UUID.randomUUID().toString().substring(0, 4) + "-" + 
               UUID.randomUUID().toString().substring(0, 4) + "-" + 
               UUID.randomUUID().toString().substring(0, 4);
    }

    private String maskCardNumber(String cardNumber) {
        String[] parts = cardNumber.split("-");
        if (parts.length == 4) {
            return parts[0] + "-****-****-" + parts[3];
        }
        return cardNumber;
    }

    private String generateCVV() {
        return String.format("%03d", (int) (Math.random() * 1000));
    }
}
