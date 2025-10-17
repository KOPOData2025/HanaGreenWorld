package com.kopo.hanacard.config;

import com.kopo.hanacard.user.domain.User;
import com.kopo.hanacard.user.repository.UserRepository;
import com.kopo.hanacard.card.domain.CardProduct;
import com.kopo.hanacard.card.repository.CardProductRepository;
import com.kopo.hanacard.card.domain.UserCard;
import com.kopo.hanacard.card.repository.UserCardRepository;
import com.kopo.hanacard.card.domain.CardBenefit;
import com.kopo.hanacard.card.repository.CardBenefitRepository;
import com.kopo.hanacard.card.domain.CardTransaction;
import com.kopo.hanacard.card.repository.CardTransactionRepository;
import com.kopo.hanacard.hanamoney.domain.HanamoneyMembership;
import com.kopo.hanacard.hanamoney.repository.HanamoneyMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerCardDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CardProductRepository cardProductRepository;
    private final UserCardRepository userCardRepository;
    private final CardBenefitRepository cardBenefitRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final HanamoneyMembershipRepository hanamoneyMembershipRepository;

    @Override
    public void run(String... args) throws Exception {
        // 테스트 고객 생성
        User testCustomer = createTestCustomer();
        
        // 고객의 카드 가입 정보 생성
        createCustomerCards(testCustomer);
        
        // 하나머니 멤버십 생성
        createHanamoneyMembership(testCustomer);
        
        // 카드 혜택 데이터 생성
        createCardBenefits();
        
        // 카드 거래내역 데이터 생성
        createCardTransactions();
    }

    private User createTestCustomer() {
        // 전화번호로 사용자 조회 (프론트엔드에서 사용할 수 있도록)
        String phoneNumber = "010-1234-5678";
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    User user = User.builder()
                            .username("green_user")
                            .name("김그린")
                            .email("green@example.com")
                            .phoneNumber(phoneNumber)
                            .birthDate("1990-05-15")
                            .ci("CI_01012345678_123456")
                            .customerGrade("GOLD")
                            .isActive(true)
                            .createdAt(LocalDateTime.now())
                            .build();
                    User savedUser = userRepository.save(user);
                    log.info("테스트 사용자 생성 완료 - ID: {}, 전화번호: {}", savedUser.getId(), phoneNumber);
                    return savedUser;
                });
    }

    private void createCustomerCards(User user) {
        // 카드 상품이 이미 존재하는지 확인 (CardProductDataInitializer에서 생성됨)

        // 고객 카드 가입 정보 생성
        if (userCardRepository.findByUserAndIsActiveTrue(user).isEmpty()) {
            List<CardProduct> availableCardProducts = cardProductRepository.findAll();
            if (!availableCardProducts.isEmpty()) {
                // CardProduct를 직접 사용하여 UserCard 생성
                List<UserCard> userCards = new ArrayList<>();
                
                // 첫 번째 카드 (그린라이프 카드)
                if (availableCardProducts.size() > 0) {
                    userCards.add(UserCard.builder()
                        .user(user)
                        .cardProduct(availableCardProducts.get(0))
                        .cardNumber("4000-1234-5678-9012")
                        .cardNumberMasked("4000-****-****-9012")
                        .expiryDate(LocalDate.now().plusYears(5))
                        .cvv("123")
                        .isActive(true)
                        .build());
                }
                
                // 두 번째 카드 (원큐씨앗 카드) - 카드 상품이 2개 이상 있을 때만 생성
                if (availableCardProducts.size() > 1) {
                    userCards.add(UserCard.builder()
                        .user(user)
                        .cardProduct(availableCardProducts.get(1))
                        .cardNumber("4000-1234-5678-9013")
                        .cardNumberMasked("4000-****-****-9013")
                        .expiryDate(LocalDate.now().plusYears(5))
                        .cvv("456")
                        .isActive(true)
                        .build());
                }
                
                if (!userCards.isEmpty()) {
                    userCardRepository.saveAll(userCards);
                    log.info("고객 {}의 카드 {}개가 가입되었습니다.", user.getName(), userCards.size());
                }
            }
        }
    }
    
    private void createCardBenefits() {
        if (cardBenefitRepository.count() == 0) {
            List<CardProduct> cardProducts = cardProductRepository.findAll();
            if (!cardProducts.isEmpty()) {
                List<CardBenefit> benefits = Arrays.asList(
                    // 그린라이프 카드 혜택
                    CardBenefit.builder()
                        .cardProduct(cardProducts.get(0))
                        .benefitType("대중교통")
                        .category("교통")
                        .cashbackRate(new BigDecimal("2.00"))
                        .discountRate(new BigDecimal("0.00"))
                        .description("지하철, 버스 이용 시 2% 캐시백")
                        .minAmount(1000L)
                        .maxAmount(50000L)
                        .build(),
                    
                    CardBenefit.builder()
                        .cardProduct(cardProducts.get(0))
                        .benefitType("친환경 가맹점")
                        .category("쇼핑")
                        .cashbackRate(new BigDecimal("1.50"))
                        .discountRate(new BigDecimal("0.00"))
                        .description("친환경 가맹점에서 1.5% 캐시백")
                        .minAmount(1000L)
                        .maxAmount(100000L)
                        .build(),
                    
                    // 원큐씨앗 카드 혜택
                    CardBenefit.builder()
                        .cardProduct(cardProducts.get(1))
                        .benefitType("원큐씨앗 적립")
                        .category("적립")
                        .cashbackRate(new BigDecimal("1.00"))
                        .discountRate(new BigDecimal("0.00"))
                        .description("모든 결제에서 1% 원큐씨앗 적립")
                        .minAmount(1000L)
                        .maxAmount(1000000L)
                        .build(),
                    
                    CardBenefit.builder()
                        .cardProduct(cardProducts.get(1))
                        .benefitType("친환경 가맹점")
                        .category("쇼핑")
                        .cashbackRate(new BigDecimal("2.00"))
                        .discountRate(new BigDecimal("0.00"))
                        .description("친환경 가맹점에서 2% 원큐씨앗 적립")
                        .minAmount(1000L)
                        .maxAmount(200000L)
                        .build()
                );
                
                cardBenefitRepository.saveAll(benefits);
                log.info("총 {}개의 카드 혜택이 생성되었습니다.", benefits.size());
            }
        }
    }
    
    private void createCardTransactions() {
        if (cardTransactionRepository.count() == 0) {
            List<UserCard> userCards = userCardRepository.findAll();
            if (!userCards.isEmpty()) {
                UserCard userCard = userCards.get(0); // 첫 번째 카드 사용

                List<CardTransaction> transactions = Arrays.asList(
                    // 전기차 충전소 거래 (친환경 가맹점 매칭)
                    CardTransaction.builder()
                        .userCard(userCard)
                        .merchantName("테슬라 충전소")
                        .category("전기차충전")
                        .merchantCategory("EV_CHARGING")
                        .amount(15000L)
                        .cashbackAmount(450L) // 3% 캐시백
                        .cashbackRate(new BigDecimal("3.00"))
                        .transactionDate(LocalDateTime.now().minusDays(1))
                        .description("전기차 급속 충전")
                        .tags("친환경,전기차")
                        .businessNumber("234-56-78901")
                        .build(),

                    // 대중교통 거래
                    CardTransaction.builder()
                        .userCard(userCard)
                        .merchantName("지하철 2호선")
                        .category("대중교통")
                        .merchantCategory("PUBLIC_TRANSPORT")
                        .amount(5000L)
                        .cashbackAmount(100L) // 2% 캐시백
                        .cashbackRate(new BigDecimal("2.00"))
                        .transactionDate(LocalDateTime.now().minusDays(2))
                        .description("지하철 이용")
                        .tags("친환경,대중교통")
                        .build(),

                    // 공유킥보드 거래
                    CardTransaction.builder()
                        .userCard(userCard)
                        .merchantName("킥고잉")
                        .category("공유킥보드")
                        .merchantCategory("SHARED_MOBILITY")
                        .amount(3000L)
                        .cashbackAmount(120L) // 4% 캐시백
                        .cashbackRate(new BigDecimal("4.00"))
                        .transactionDate(LocalDateTime.now().minusDays(3))
                        .description("공유킥보드 이용")
                        .tags("친환경,공유킥보드")
                        .build(),

                    // 리필샵 거래
                    CardTransaction.builder()
                        .userCard(userCard)
                        .merchantName("제로웨이스트샵")
                        .category("리필샵")
                        .merchantCategory("REFILL_STATION")
                        .amount(25000L)
                        .cashbackAmount(750L) // 3% 캐시백
                        .cashbackRate(new BigDecimal("3.00"))
                        .transactionDate(LocalDateTime.now().minusDays(4))
                        .description("리필 용기 사용")
                        .tags("친환경,리필샵")
                        .build(),

                    // 유기농 식품 거래
                    CardTransaction.builder()
                        .userCard(userCard)
                        .merchantName("올가홀푸드")
                        .category("유기농식품")
                        .merchantCategory("ECO_BRAND")
                        .amount(35000L)
                        .cashbackAmount(1050L) // 3% 캐시백
                        .cashbackRate(new BigDecimal("3.00"))
                        .transactionDate(LocalDateTime.now().minusDays(5))
                        .description("유기농 채소 구매")
                        .tags("친환경,유기농")
                        .build(),

                    // 중고거래 거래
                    CardTransaction.builder()
                        .userCard(userCard)
                        .merchantName("당근마켓")
                        .category("중고거래")
                        .merchantCategory("ECO_BRAND")
                        .amount(8000L)
                        .cashbackAmount(120L) // 1.5% 캐시백
                        .cashbackRate(new BigDecimal("1.50"))
                        .transactionDate(LocalDateTime.now().minusDays(6))
                        .description("중고 물품 구매")
                        .tags("친환경,중고거래")
                        .build(),

                    // 친환경 브랜드 거래
                    CardTransaction.builder()
                        .userCard(userCard)
                        .merchantName("네이처리퍼블릭")
                        .category("친환경브랜드")
                        .merchantCategory("ECO_BRAND")
                        .amount(120000L)
                        .cashbackAmount(2400L) // 2% 캐시백
                        .cashbackRate(new BigDecimal("2.00"))
                        .transactionDate(LocalDateTime.now().minusDays(7))
                        .description("친환경 화장품 구매")
                        .tags("친환경,브랜드")
                        .build(),

                    // 현대 전기차 충전
                    CardTransaction.builder()
                        .userCard(userCard)
                        .merchantName("현대전기차")
                        .category("전기차")
                        .merchantCategory("EV_CHARGING")
                        .amount(50000L)
                        .cashbackAmount(2500L) // 5% 캐시백 (프리미엄)
                        .cashbackRate(new BigDecimal("5.00"))
                        .transactionDate(LocalDateTime.now().minusDays(8))
                        .description("전기차 급속 충전")
                        .tags("친환경,전기차,프리미엄")
                        .build(),

                    // 라임 스쿠터
                    CardTransaction.builder()
                        .userCard(userCard)
                        .merchantName("라임 스쿠터")
                        .category("공유킥보드")
                        .merchantCategory("SHARED_MOBILITY")
                        .amount(2500L)
                        .cashbackAmount(100L) // 4% 캐시백
                        .cashbackRate(new BigDecimal("4.00"))
                        .transactionDate(LocalDateTime.now().minusDays(9))
                        .description("전기 스쿠터 이용")
                        .tags("친환경,전기스쿠터")
                        .build(),

                    // 이마트 유기농
                    CardTransaction.builder()
                        .userCard(userCard)
                        .merchantName("이마트 유기농")
                        .category("유기농식품")
                        .merchantCategory("ECO_BRAND")
                        .amount(32000L)
                        .cashbackAmount(960L) // 3% 캐시백
                        .cashbackRate(new BigDecimal("3.00"))
                        .transactionDate(LocalDateTime.now().minusDays(10))
                        .description("유기농 채소 구매")
                        .tags("친환경,유기농")
                        .build()
                );

                cardTransactionRepository.saveAll(transactions);
            }
        }
    }

    private void createHanamoneyMembership(User user) {
        // 이미 하나머니 멤버십이 있는지 확인
        if (hanamoneyMembershipRepository.findByUser_Id(user.getId()).isEmpty()) {
            HanamoneyMembership membership = HanamoneyMembership.builder()
                    .user(user)
                    .membershipId("HM_" + user.getId() + "_" + System.currentTimeMillis())
                    .balance(50000L) // 초기 잔액 5만원
                    .totalEarned(50000L) // 총 적립 5만원
                    .totalSpent(0L) // 총 사용 0원
                    .isActive(true)
                    .membershipLevel("BASIC")
                    .build();
            
            hanamoneyMembershipRepository.save(membership);
            log.info("하나머니 멤버십 생성 완료 - 사용자ID: {}, 잔액: {}", user.getId(), membership.getBalance());
        } else {
            log.info("하나머니 멤버십이 이미 존재합니다 - 사용자ID: {}", user.getId());
        }
    }
}
