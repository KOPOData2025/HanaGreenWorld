package com.kopo.hanacard.integration.service;

import com.kopo.hanacard.card.domain.UserCard;
import com.kopo.hanacard.card.domain.CardTransaction;
import com.kopo.hanacard.card.repository.UserCardRepository;
import com.kopo.hanacard.card.repository.CardTransactionRepository;
import com.kopo.hanacard.user.domain.User;
import com.kopo.hanacard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardIntegrationService {

    private final UserCardRepository userCardRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final UserRepository userRepository;

    public Map<String, Object> getCardInfo(Long memberId) {
        try {
            // 사용자 조회
            Optional<User> userOpt = userRepository.findById(memberId);
            if (userOpt.isEmpty()) {
                return createEmptyCardResponse();
            }

            User user = userOpt.get();

            List<UserCard> userCards = userCardRepository.findByUserIdAndIsActiveTrue(memberId);
            
            List<Map<String, Object>> cards = new ArrayList<>();
            BigDecimal totalCreditLimit = BigDecimal.ZERO;
            BigDecimal totalAvailableLimit = BigDecimal.ZERO;
            BigDecimal monthlyTotalUsage = BigDecimal.ZERO;
            
            for (UserCard userCard : userCards) {
                // 카드 정보 매핑
                BigDecimal creditLimit = new BigDecimal(userCard.getCardProduct().getCreditLimit());
                BigDecimal availableLimit = creditLimit.subtract(new BigDecimal("1000000")); // 임시 계산
                
                Map<String, Object> cardInfo = new HashMap<>();
                cardInfo.put("cardNumber", userCard.getCardNumberMasked());
                cardInfo.put("cardName", userCard.getCardProduct().getProductName());
                cardInfo.put("cardType", userCard.getCardProduct().getProductType());
                cardInfo.put("cardStatus", userCard.getIsActive() ? "ACTIVE" : "INACTIVE");
                cardInfo.put("creditLimit", creditLimit);
                cardInfo.put("availableLimit", availableLimit);
                cardInfo.put("monthlyUsage", new BigDecimal("1000000")); // 임시 데이터
                cardInfo.put("issueDate", userCard.getCreatedAt());
                cardInfo.put("expiryDate", userCard.getExpiryDate().atStartOfDay());
                cardInfo.put("benefits", List.of("주유할인 5%", "커피할인 30%", "친환경 적립")); // 임시 데이터
                // 실제 카드 이미지 URL 사용 (데이터베이스에 저장된 이미지 URL)
                String cardImageUrl = userCard.getCardProduct().getImageUrl();
                if (cardImageUrl == null || cardImageUrl.isEmpty()) {
                    // 이미지 URL이 없으면 기본 placeholder 사용
                    cardImageUrl = "https://via.placeholder.com/300x200/138072/FFFFFF?text=" + userCard.getCardProduct().getProductName().replace(" ", "+");
                }
                cardInfo.put("cardImageUrl", cardImageUrl);
                cardInfo.put("cardImageBase64", null);
                
                cards.add(cardInfo);
                
                // 합계 계산
                totalCreditLimit = totalCreditLimit.add(creditLimit);
                totalAvailableLimit = totalAvailableLimit.add(availableLimit);
                monthlyTotalUsage = monthlyTotalUsage.add(new BigDecimal("1000000"));
            }
            
            // 요약 정보 생성
            Map<String, Object> summary = Map.of(
                "totalCardCount", cards.size(),
                "activeCardCount", cards.size(),
                "totalCreditLimit", totalCreditLimit,
                "totalAvailableLimit", totalAvailableLimit,
                "monthlyTotalUsage", monthlyTotalUsage,
                "primaryCardType", cards.isEmpty() ? "NONE" : cards.get(0).get("cardType")
            );

            return Map.of(
                "cards", cards,
                "summary", summary,
                "responseTime", LocalDateTime.now()
            );
            
        } catch (Exception e) {
            return createEmptyCardResponse();
        }
    }

    public Map<String, Object> getCustomerInfo(Long userId) {
        try {
            // 사용자 조회
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return createEmptyCustomerResponse();
            }
            
            User user = userOpt.get();
            log.info("사용자 정보 조회 완료 - 이름: {}, 이메일: {}, 전화번호: {}",
                    user.getName(), user.getEmail(), user.getPhoneNumber());
            
            // 사용자의 활성 카드 조회
            List<UserCard> userCards = userCardRepository.findByUserIdAndIsActiveTrue(userId);
            log.info("조회된 카드 수: {}", userCards.size());
            
            // 고객 기본 정보
            Map<String, Object> customerInfo = Map.of(
                "name", user.getName(),
                "email", user.getEmail(),
                "phoneNumber", user.getPhoneNumber(),
                "customerGrade", "GOLD", // 임시 등급
                "joinDate", user.getCreatedAt(),
                "isActive", true,
                "totalCreditLimit", new BigDecimal("50000000"), // 임시 데이터
                "usedCreditAmount", new BigDecimal("10000000") // 임시 데이터
            );
            
            // 카드 정보 목록
            List<Map<String, Object>> cards = new ArrayList<>();
            for (UserCard userCard : userCards) {
                List<Map<String, Object>> cardBenefits = getCardBenefits(userCard.getCardProduct().getProductId());

                List<Map<String, Object>> cardTransactions = getCardTransactionsInternal(userId, userCard.getId());
                
                Map<String, Object> cardInfo = new HashMap<>();
                cardInfo.put("cardNumber", userCard.getCardNumberMasked());
                cardInfo.put("cardName", userCard.getCardProduct().getProductName());
                cardInfo.put("cardType", userCard.getCardProduct().getProductType());
                cardInfo.put("cardStatus", userCard.getIsActive() ? "ACTIVE" : "INACTIVE");
                cardInfo.put("creditLimit", new BigDecimal(userCard.getCardProduct().getCreditLimit()));
                cardInfo.put("availableLimit", new BigDecimal("40000000")); // 임시 데이터
                cardInfo.put("issueDate", userCard.getCreatedAt());
                cardInfo.put("expiryDate", userCard.getExpiryDate().atStartOfDay());
                cardInfo.put("benefits", cardBenefits); // 실제 혜택 데이터
                cardInfo.put("transactions", cardTransactions); // 실제 거래내역 데이터
                cardInfo.put("monthlyUsage", new BigDecimal("1000000")); // 임시 데이터
                cards.add(cardInfo);
            }
            
            // 하나머니 정보 (임시 데이터)
            Map<String, Object> hanamoneyInfo = Map.of(
                "membershipLevel", "GOLD",
                "currentPoints", 50000,
                "accumulatedPoints", 200000,
                "isSubscribed", true,
                "joinDate", user.getCreatedAt()
            );
            
            log.info("고객 정보 조회 완료 - 사용자ID: {}, 카드 수: {}", userId, cards.size());
            
            return Map.of(
                "customerInfo", customerInfo,
                "cards", cards,
                "hanamoneyInfo", hanamoneyInfo,
                "responseTime", LocalDateTime.now()
            );
            
        } catch (Exception e) {
            log.error("고객 정보 조회 실패 - 사용자ID: {}", userId, e);
            return createEmptyCustomerResponse();
        }
    }

    private List<Map<String, Object>> getCardBenefits(Long productId) {
        try {
            return List.of(
                Map.of(
                    "benefitType", "친환경 교통",
                    "category", "대중교통",
                    "cashbackRate", 2.0,
                    "description", "지하철, 버스 이용 시 2% 캐시백"
                ),
                Map.of(
                    "benefitType", "친환경 가맹점",
                    "category", "쇼핑",
                    "cashbackRate", 1.5,
                    "description", "친환경 가맹점에서 1.5% 캐시백"
                )
            );
        } catch (Exception e) {
            log.error("카드 혜택 조회 실패 - 상품ID: {}", productId, e);
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getCardTransactions(Long userId) {
        log.info("카드 거래내역 조회 시작 - 사용자ID: {}", userId);
        
        try {
            // 사용자 카드 조회
            List<UserCard> userCards = userCardRepository.findByUserIdAndIsActiveTrue(userId);
            
            List<Map<String, Object>> allTransactions = new ArrayList<>();
            
            for (UserCard userCard : userCards) {
                List<Map<String, Object>> cardTransactions = getCardTransactionsInternal(userId, userCard.getId());
                allTransactions.addAll(cardTransactions);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("transactions", allTransactions);
            response.put("totalCount", allTransactions.size());
            response.put("userId", userId);
            
            log.info("카드 거래내역 조회 성공 - 사용자ID: {}, 거래건수: {}", userId, allTransactions.size());
            return response;
            
        } catch (Exception e) {
            log.error("카드 거래내역 조회 실패 - 사용자ID: {}", userId, e);
            return Map.of("transactions", new ArrayList<>(), "totalCount", 0, "userId", userId);
        }
    }
    

    public Map<String, Object> getConsumptionSummary(Long userId) {
        log.info("월간 소비현황 조회 시작 - 사용자ID: {}", userId);
        
        try {
            // 사용자 카드 조회
            List<UserCard> userCards = userCardRepository.findByUserIdAndIsActiveTrue(userId);
            
            // 이번 달 첫날부터 현재까지
            LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = LocalDateTime.now();
            
            // 카테고리별 소비 금액 계산
            Map<String, Long> categoryAmounts = new HashMap<>();
            long totalAmount = 0;
            long totalCashback = 0;
            
            for (UserCard userCard : userCards) {
                // 이번달 거래내역만 조회
                List<CardTransaction> monthlyTransactions = cardTransactionRepository
                    .findByUserCardAndTransactionDateBetweenOrderByTransactionDateDesc(
                        userCard, startOfMonth, endOfMonth);
                
                for (CardTransaction transaction : monthlyTransactions) {
                    String category = transaction.getCategory();
                    Long amount = transaction.getAmount();
                    Long cashback = transaction.getCashbackAmount();
                    
                    categoryAmounts.merge(category, amount, Long::sum);
                    totalAmount += amount;
                    totalCashback += cashback;
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalAmount", totalAmount);
            response.put("totalCashback", totalCashback);
            response.put("categoryAmounts", categoryAmounts);
            response.put("userId", userId);
            
            log.info("월간 소비현황 조회 성공 - 사용자ID: {}, 총소비: {}, 총캐시백: {}, 이번달 거래건수: {}",
                    userId, totalAmount, totalCashback, categoryAmounts.values().stream().mapToLong(Long::longValue).sum());
            return response;
            
        } catch (Exception e) {
            return Map.of("totalAmount", 0, "totalCashback", 0, "categoryAmounts", new HashMap<>(), "userId", userId);
        }
    }

    private List<Map<String, Object>> getCardTransactionsInternal(Long userId, Long cardId) {
        try {
            UserCard userCard = userCardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("사용자 카드를 찾을 수 없습니다: " + cardId));

            List<CardTransaction> transactions = cardTransactionRepository.findByUserCard(userCard);

            List<Map<String, Object>> result = new ArrayList<>();
            for (CardTransaction transaction : transactions) {
                Map<String, Object> transactionMap = new HashMap<>();
                transactionMap.put("transactionDate", transaction.getTransactionDate().toString());
                transactionMap.put("merchantName", transaction.getMerchantName());
                transactionMap.put("category", transaction.getCategory());
                transactionMap.put("amount", transaction.getAmount().longValue());
                transactionMap.put("cashbackAmount", transaction.getCashbackAmount().longValue());
                transactionMap.put("cashbackRate", transaction.getCashbackRate().doubleValue());
                transactionMap.put("description", transaction.getDescription());
                transactionMap.put("merchantCategory", transaction.getMerchantCategory());

                result.add(transactionMap);
            }
            return result;
            
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Map<String, Object> createEmptyCardResponse() {
        return Map.of(
            "cards", List.of(),
            "summary", Map.of(
                "totalCardCount", 0,
                "activeCardCount", 0,
                "totalCreditLimit", BigDecimal.ZERO,
                "totalAvailableLimit", BigDecimal.ZERO,
                "monthlyTotalUsage", BigDecimal.ZERO,
                "primaryCardType", "NONE"
            ),
            "responseTime", LocalDateTime.now()
        );
    }

    private Map<String, Object> createEmptyCustomerResponse() {
        return Map.of(
            "customerInfo", Map.of(
                "name", "",
                "email", "",
                "phoneNumber", "",
                "customerGrade", "NONE",
                "joinDate", LocalDateTime.now(),
                "isActive", false,
                "totalCreditLimit", BigDecimal.ZERO,
                "usedCreditAmount", BigDecimal.ZERO
            ),
            "cards", List.of(),
            "hanamoneyInfo", Map.of(
                "membershipLevel", "NONE",
                "currentPoints", 0,
                "accumulatedPoints", 0,
                "isSubscribed", false,
                "joinDate", LocalDateTime.now()
            ),
            "responseTime", LocalDateTime.now()
        );
    }
}