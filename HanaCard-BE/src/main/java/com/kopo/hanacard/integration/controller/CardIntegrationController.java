package com.kopo.hanacard.integration.controller;

import com.kopo.hanacard.common.dto.ApiResponse;
import com.kopo.hanacard.hanamoney.domain.HanamoneyMembership;
import com.kopo.hanacard.hanamoney.dto.HanamoneyMembershipResponse;
import com.kopo.hanacard.hanamoney.service.HanamoneyService;
import com.kopo.hanacard.integration.service.CardIntegrationService;
import com.kopo.hanacard.user.domain.User;
import com.kopo.hanacard.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Card Integration API", description = "하나카드 통합 정보 조회 API")
public class CardIntegrationController {

    private final HanamoneyService hanamoneyService;
    private final CardIntegrationService cardIntegrationService;
    private final UserRepository userRepository;

    @PostMapping("/hanamoney-info")
    @Operation(
        summary = "하나머니 정보 조회 (통합)",
        description = "하나그린세상에서 하나머니 정보를 조회하는 통합 API입니다."
    )
    public ResponseEntity<Map<String, Object>> getHanamoneyInfo(
            @RequestBody Map<String, String> requestBody) {
        
        try {
            if (!"GREEN_WORLD".equals(requestBody.get("requestingService"))) {
                throw new IllegalArgumentException("허용되지 않은 요청 서비스입니다.");
            }

            Long userId = extractMemberIdFromRequest(requestBody);

            HanamoneyMembership membership = hanamoneyService.getHanamoneyMembershipByUserId(userId);
            
            log.info("하나머니 멤버십 조회 성공 - 사용자ID: {}, 잔액: {}, 총적립: {}", 
                    userId, membership.getBalance(), membership.getTotalEarned());

            Map<String, Object> responseData = Map.of(
                "membershipLevel", membership.getMembershipLevel(),
                "currentPoints", membership.getBalance(),
                "accumulatedPoints", membership.getTotalEarned(),
                "isSubscribed", membership.isActive(),
                "joinDate", membership.getCreatedAt().toString()
            );
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "하나머니 정보 조회 성공",
                "data", responseData
            );

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "하나머니 정보 조회 실패: " + (e.getMessage() != null ? e.getMessage() : "알 수 없는 오류"));
            errorResponse.put("data", null);
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/cards/{memberId}")
    @Operation(
        summary = "카드 정보 조회 (통합)",
        description = "하나그린세상에서 카드 정보를 조회하는 통합 API입니다."
    )
    public ResponseEntity<Map<String, Object>> getCardInfo(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "true") Boolean consent) {
        
        try {
            if (!Boolean.TRUE.equals(consent)) {
                throw new IllegalArgumentException("고객 동의가 필요합니다.");
            }
            
            log.info("카드 정보 조회 요청 - 회원ID: {}", memberId);

            Map<String, Object> cardData = cardIntegrationService.getCardInfo(memberId);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "카드 정보 조회 성공",
                "data", cardData
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("카드 정보 조회 실패", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "카드 정보 조회 실패: " + (e.getMessage() != null ? e.getMessage() : "알 수 없는 오류"));
            errorResponse.put("data", null);
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/customer-info")
    @Operation(
        summary = "고객 정보 조회 (통합)",
        description = "하나그린세상에서 고객 정보를 조회하는 통합 API입니다."
    )
    public ResponseEntity<Map<String, Object>> getCustomerInfo(
            @RequestBody Map<String, String> requestBody) {
        
        try {
            if (!"GREEN_WORLD".equals(requestBody.get("requestingService"))) {
                throw new IllegalArgumentException("허용되지 않은 요청 서비스입니다.");
            }

            Long userId = extractMemberIdFromRequest(requestBody);

            Map<String, Object> customerData = cardIntegrationService.getCustomerInfo(userId);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "고객 정보 조회 성공",
                "data", customerData
            );
            
            log.info("고객 정보 조회 성공 - 사용자ID: {}", userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("고객 정보 조회 실패", e);
            
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "고객 정보 조회 실패: " + e.getMessage(),
                "data", null
            );
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/cards/{memberId}/transactions")
    @Operation(
        summary = "카드 거래내역 조회 (통합)",
        description = "하나그린세상에서 카드 거래내역을 조회하는 통합 API입니다."
    )
    public ResponseEntity<Map<String, Object>> getCardTransactions(@PathVariable Long memberId) {
        try {
            Map<String, Object> transactionData = cardIntegrationService.getCardTransactions(memberId);
            log.info("[통합 API] 거래내역 조회 결과: {}", transactionData);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "카드 거래내역 조회 성공",
                "data", transactionData
            );
            
            log.info("카드 거래내역 조회 성공 - 회원ID: {}", memberId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "카드 거래내역 조회 실패: " + e.getMessage(),
                "data", null
            );
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/cards/{memberId}/consumption/summary")
    @Operation(
        summary = "월간 소비현황 조회 (통합)",
        description = "하나그린세상에서 월간 소비현황을 조회하는 통합 API입니다."
    )
    public ResponseEntity<Map<String, Object>> getConsumptionSummary(@PathVariable Long memberId) {
        try {
            Map<String, Object> consumptionData = cardIntegrationService.getConsumptionSummary(memberId);
            log.info("[통합 API] 소비현황 조회 결과: {}", consumptionData);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "월간 소비현황 조회 성공",
                "data", consumptionData
            );

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "월간 소비현황 조회 실패: " + e.getMessage(),
                "data", null
            );
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/hanamoney-earn")
    @Operation(
        summary = "하나머니 적립 (통합)",
        description = "하나그린세상에서 하나머니를 적립하는 통합 API입니다."
    )
    public ResponseEntity<Map<String, Object>> earnHanamoney(
            @RequestBody Map<String, Object> requestBody) {
        
        try {
            if (!"GREEN_WORLD".equals(requestBody.get("requestingService"))) {
                throw new IllegalArgumentException("허용되지 않은 요청 서비스입니다.");
            }

            Long userId = extractUserIdFromToken(requestBody.get("customerInfoToken").toString());
            Long amount = Long.valueOf(requestBody.get("amount").toString());
            String description = requestBody.get("description").toString();
            
            // 하나머니 적립
            HanamoneyMembership membership = hanamoneyService.earn(userId, amount, description);
            
            log.info("하나머니 적립 성공 - 사용자ID: {}, 적립금액: {}, 잔액: {}", 
                    userId, amount, membership.getBalance());

            Map<String, Object> responseData = Map.of(
                "membershipLevel", membership.getMembershipLevel(),
                "currentPoints", membership.getBalance(),
                "accumulatedPoints", membership.getTotalEarned(),
                "isSubscribed", membership.isActive(),
                "joinDate", membership.getCreatedAt().toString()
            );
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "하나머니 적립 성공",
                "data", responseData
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "하나머니 적립 실패: " + e.getMessage(),
                "data", null
            );
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    private Long extractUserIdFromToken(String customerInfoToken) {
        if (customerInfoToken == null || customerInfoToken.isEmpty()) {
            throw new IllegalArgumentException("고객 정보 토큰이 필요합니다.");
        }
        
        try {
            String ci = new String(java.util.Base64.getDecoder().decode(customerInfoToken));

            Optional<User> userOpt = userRepository.findByCi(ci);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                return user.getId();
            } else {
                throw new IllegalArgumentException("유효하지 않은 고객 정보 토큰입니다.");
            }
        } catch (Exception e) {
            log.error("customerInfoToken에서 사용자 ID 추출 실패", e);
            throw new IllegalArgumentException("고객 정보 토큰 처리 중 오류가 발생했습니다.");
        }
    }

    private Long extractMemberIdFromRequest(Map<String, String> requestBody) {
        // 1. 직접 memberId가 있는 경우
        String memberIdStr = requestBody.get("memberId");
        if (memberIdStr != null && !memberIdStr.isEmpty()) {
            try {
                return Long.valueOf(memberIdStr);
            } catch (NumberFormatException e) {
                log.warn("잘못된 memberId 형식: {}", memberIdStr);
            }
        }
        
        // 2. customerInfoToken에서 CI 추출하여 사용자 조회
        String customerInfoToken = requestBody.get("customerInfoToken");
        if (customerInfoToken != null && !customerInfoToken.isEmpty()) {
            try {
                // Base64 디코딩하여 CI 추출
                String ci = new String(java.util.Base64.getDecoder().decode(customerInfoToken));
                log.info("추출된 CI: {}", maskCi(ci));
                
                // CI로 사용자 조회
                Optional<User> userOpt = userRepository.findByCi(ci);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    log.info("CI 기반 사용자 조회 성공: ID={}, CI={}", user.getId(), maskCi(ci));
                    return user.getId();
                } else {
                    log.warn("CI에 해당하는 사용자를 찾을 수 없음: CI={}", maskCi(ci));
                }
            } catch (Exception e) {
                log.error("customerInfoToken에서 CI 추출 실패", e);
            }
        }
        
        // 3. 기본값 반환
        log.warn("memberId 추출 실패, 기본값 사용: 1L");
        return 1L;
    }
    
    /**
     * CI 마스킹 (로그용)
     */
    private String maskCi(String ci) {
        if (ci == null || ci.length() < 8) {
            return "****";
        }
        return ci.substring(0, 4) + "****" + ci.substring(ci.length() - 4);
    }
}