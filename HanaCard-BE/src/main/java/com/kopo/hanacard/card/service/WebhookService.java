package com.kopo.hanacard.card.service;

import com.kopo.hanacard.card.domain.CardTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final RestTemplate restTemplate;

    @Value("${integration.greenworld.url:http://localhost:8080}")
    private String greenWorldBaseUrl;

    public void sendCardTransactionWebhook(CardTransaction transaction) {
        try {
            String webhookUrl = greenWorldBaseUrl + "/api/integration/webhook/card-transaction";

            Map<String, Object> webhookData = new HashMap<>();
            webhookData.put("transactionId", transaction.getId());
            webhookData.put("userId", transaction.getUserCard().getUser().getId());
            webhookData.put("merchantName", transaction.getMerchantName());
            webhookData.put("businessNumber", transaction.getBusinessNumber());
            webhookData.put("amount", transaction.getAmount());
            webhookData.put("category", transaction.getCategory());
            webhookData.put("merchantCategory", transaction.getMerchantCategory());
            webhookData.put("transactionDate", transaction.getTransactionDate().toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Source", "HANACARD");
            headers.set("X-Webhook-Type", "CARD_TRANSACTION");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(webhookData, headers);

            log.info("웹훅 전송 요청 - URL: {}, 데이터: {}", webhookUrl, webhookData);

            ResponseEntity<Map> response = restTemplate.exchange(
                webhookUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("하나그린세상 웹훅 전송 성공 - 거래ID: {}, 응답: {}",
                        transaction.getId(), response.getBody());
            } else {
                log.warn("⚠하나그린세상 웹훅 전송 실패 - 거래ID: {}, 상태코드: {}",
                        transaction.getId(), response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("하나그린세상 웹훅 전송 실패 - 거래ID: {}, 에러: {}",
                    transaction.getId(), e.getMessage(), e);
            // 웹훅 실패는 전체 트랜잭션을 롤백하지 않음
        }
    }
}
