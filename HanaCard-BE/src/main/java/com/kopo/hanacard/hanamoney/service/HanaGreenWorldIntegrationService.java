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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HanaGreenWorldIntegrationService {

    private final HanamoneyMembershipRepository hanamoneyMembershipRepository;
    private final HanamoneyTransactionRepository hanamoneyTransactionRepository;
    private final UserService userService;
    private final RestTemplate restTemplate;

    @Value("${integration.hanagreenworld.url}")
    private String hanaGreenWorldBaseUrl;

    @Transactional
    public void syncToGreenWorld(Long userId, Long amount, String transactionType, String description) {
        try {
            User user = userService.getUserById(userId);
            String url = hanaGreenWorldBaseUrl + "/api/members/update-hana-money";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phoneNumber", user.getPhoneNumber());
            requestBody.put("amount", amount);
            requestBody.put("transactionType", transactionType);
            requestBody.put("description", description);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            
            log.info("하나그린세상 동기화 완료. userId: {}, amount: {}, type: {}", userId, amount, transactionType);
            
        } catch (Exception e) {
            log.error("하나그린세상 동기화 실패. userId: {}, amount: {}, type: {}", userId, amount, transactionType, e);
            // 동기화 실패해도 하나카드 내부 처리는 계속 진행
        }
    }
}
