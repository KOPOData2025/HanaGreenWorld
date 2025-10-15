package com.kopo.hanacard.card.controller;

import com.kopo.hanacard.card.domain.CardTransaction;
import com.kopo.hanacard.card.service.CardTransactionService;
import com.kopo.hanacard.card.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/card-transactions")
@RequiredArgsConstructor
public class CardTransactionController {

    private final CardTransactionService cardTransactionService;
    private final WebhookService webhookService;

    @PostMapping
    @Operation(
        summary = "카드 거래 생성",
        description = "새로운 카드 거래를 생성하고 친환경 가맹점 매칭 이벤트를 발행합니다."
    )
    public ResponseEntity<Map<String, Object>> createCardTransaction(
            @RequestBody Map<String, Object> request) {

        try {
            Long userId = ((Number) request.get("userId")).longValue();
            String merchantName = (String) request.get("merchantName");
            String businessNumber = (String) request.get("businessNumber");
            Long amount = ((Number) request.get("amount")).longValue();
            String category = (String) request.get("category");
            String merchantCategory = (String) request.get("merchantCategory");

            CardTransaction transaction = cardTransactionService.createCardTransaction(
                userId, merchantName, businessNumber, amount, category, merchantCategory);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "transactionId", transaction.getId(),
                "message", "카드 거래가 성공적으로 생성되었습니다."
            ));

        } catch (Exception e) {
            log.error("카드 거래 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/test-eco-merchant")
    @Operation(
        summary = "친환경 가맹점 거래 테스트",
        description = "친환경 가맹점에서의 거래를 테스트로 생성합니다."
    )
    public ResponseEntity<Map<String, Object>> createEcoMerchantTransaction(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "그린마트 철산점") String merchantName,
            @RequestParam(defaultValue = "123-45-67890") String businessNumber,
            @RequestParam(defaultValue = "25000") Long amount) {

        try {
            CardTransaction transaction = cardTransactionService.createCardTransaction(
                userId, merchantName, businessNumber, amount, "식품", "ECO_FOOD");

            return ResponseEntity.ok(Map.of(
                "success", true,
                "transactionId", transaction.getId(),
                "message", String.format("%s에서 %d원 거래가 생성되었습니다.", merchantName, amount)
            ));

        } catch (Exception e) {
            log.error("친환경 가맹점 거래 테스트 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}
