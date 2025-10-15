package com.kopo.hanacard.card.config;

import com.kopo.hanacard.card.domain.CardProduct;
import com.kopo.hanacard.card.repository.CardProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardProductDataInitializer implements CommandLineRunner {

    private final CardProductRepository cardProductRepository;

    @Override
    public void run(String... args) throws Exception {
        if (cardProductRepository.count() == 0) {
            log.info("카드 상품 더미데이터 초기화 시작...");
            initializeCardProducts();
            log.info("카드 상품 더미데이터 초기화 완료!");
        } else {
            log.info("카드 상품 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
        }
    }

    private void initializeCardProducts() {
        List<CardProduct> cardProducts = Arrays.asList(
            // 그린라이프 카드
            new CardProduct(
                "하나 그린라이프 카드",
                "GREEN_LIFESTYLE",
                "친환경 라이프스타일을 위한 카드로, 대중교통 이용 시 2% 적립, 친환경 가맹점에서 1.5% 적립 혜택을 제공합니다.",
                30000L,
                5000000L,
                "대중교통 2% 적립, 친환경 가맹점 1.5% 적립, 전국 가맹점 0.5% 적립",
                "hana_greenlife_card.png"
            ),

            // 원큐씨앗 카드
            new CardProduct(
                "하나 원큐씨앗 카드",
                "ECO_SEED",
                "원큐씨앗 적립에 특화된 카드로, 모든 결제에서 원큐씨앗을 적립받을 수 있습니다.",
                25000L,
                3000000L,
                "모든 결제 1% 원큐씨앗 적립, 친환경 가맹점 2% 원큐씨앗 적립",
                "hana_greenlife_card.png"
            ),

            // 제로웨이스트 카드
            new CardProduct(
                "하나 제로웨이스트 카드",
                "ZERO_WASTE",
                "제로웨이스트 라이프를 위한 카드로, 리필샵, 무포장샵에서 특별 혜택을 제공합니다.",
                20000L,
                2000000L,
                "리필샵 3% 적립, 무포장샵 2% 적립, 친환경 브랜드 1.5% 적립",
                "hana_greenlife_card.png"
            ),

            // 탄소중립 카드
            new CardProduct(
                "하나 탄소중립 카드",
                "CARBON_NEUTRAL",
                "탄소중립 실현을 위한 카드로, 전기차 충전, 재생에너지 구매 시 혜택을 제공합니다.",
                35000L,
                7000000L,
                "전기차 충전 2% 적립, 재생에너지 구매 1.5% 적립, 공공자전거 2% 적립",
                "hana_greenlife_card.png"
            ),

            // 에코투어 카드
            new CardProduct(
                "하나 에코투어 카드",
                "ECO_TOUR",
                "친환경 여행을 위한 카드로, 친환경 숙박, 대중교통 이용 시 특별 혜택을 제공합니다.",
                28000L,
                4000000L,
                "친환경 숙박 2% 적립, 대중교통 1.5% 적립, 친환경 관광지 1% 적립",
                "hana_greenlife_card.png"
            ),

            // 그린인베스트 카드
            new CardProduct(
                "하나 그린인베스트 카드",
                "GREEN_INVEST",
                "친환경 투자를 위한 프리미엄 카드로, ESG 투자, 친환경 펀드 가입 시 혜택을 제공합니다.",
                50000L,
                10000000L,
                "ESG 투자 2% 적립, 친환경 펀드 가입 1.5% 적립, 프리미엄 서비스 제공",
                "hana_greenlife_card.png"
            )
        );

        cardProductRepository.saveAll(cardProducts);
        log.info("총 {}개의 카드 상품이 생성되었습니다.", cardProducts.size());
    }
}


