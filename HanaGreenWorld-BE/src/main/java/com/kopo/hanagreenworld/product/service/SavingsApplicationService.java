package com.kopo.hanagreenworld.product.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.kopo.hanagreenworld.member.domain.Member;
import com.kopo.hanagreenworld.member.domain.MemberProfile;
import com.kopo.hanagreenworld.member.repository.MemberRepository;
import com.kopo.hanagreenworld.member.repository.MemberProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SavingsApplicationService {

    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${integration.bank.url}")
    private String hanabankApiUrl;

    @Value("${internal.service.secret}")
    private String secret;

    @Transactional
    public SavingsApplicationResponse processSavingsApplication(Long userId, CreateSavingsRequest request) {

        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 우대금리 계산
        BigDecimal preferentialRate = calculatePreferentialRate(userId);

        try {
            String ci = member.getCi();
            if (ci == null || ci.trim().isEmpty()) {
                ci = "CI_" + member.getPhoneNumber().replace("-", "") + "_" + member.getName().hashCode();
            }

            String customerInfoToken = java.util.Base64.getEncoder().encodeToString(ci.getBytes());

            Map<String, Object> hanabankRequest = new HashMap<>();
            hanabankRequest.put("customerInfoToken", customerInfoToken);
            hanabankRequest.put("productId", request.getSavingProductId());
            hanabankRequest.put("preferentialRate", preferentialRate);
            hanabankRequest.put("applicationAmount", request.getApplicationAmount());
            
            // 자동이체 설정 추가
            hanabankRequest.put("autoTransferEnabled", request.getAutoTransferEnabled() != null ? request.getAutoTransferEnabled() : false);
            hanabankRequest.put("transferDay", request.getTransferDay());
            hanabankRequest.put("monthlyTransferAmount", request.getMonthlyTransferAmount());
            hanabankRequest.put("withdrawalAccountNumber", request.getWithdrawalAccountNumber());
            hanabankRequest.put("withdrawalBankName", request.getWithdrawalBankName());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // 하나은행 내부 서비스 인증 헤더 추가
            String internalAuthToken = java.util.Base64.getEncoder().encodeToString(secret.getBytes());
            headers.set("X-Internal-Service", internalAuthToken);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(hanabankRequest, headers);

            String apiUrl = hanabankApiUrl + "/api/integration/savings-accounts";
            log.info("하나은행 API 호출: {}", apiUrl);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl, 
                HttpMethod.POST, 
                entity, 
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                log.info("하나은행 응답: {}", responseBody);
            } else {
                throw new RuntimeException("하나은행 적금 계좌 생성에 실패했습니다.");
            }

        } catch (Exception e) {
            throw new RuntimeException("적금 계좌 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }

        return SavingsApplicationResponse.builder()
                .userId(userId)
                .userName(member.getName())
                .phoneNumber(member.getPhoneNumber())
                .productId(request.getSavingProductId())
                .applicationAmount(request.getApplicationAmount())
                .preferentialRate(preferentialRate)
                .autoTransferEnabled(request.getAutoTransferEnabled())
                .transferDay(request.getTransferDay())
                .monthlyTransferAmount(request.getMonthlyTransferAmount())
                .withdrawalAccountNumber(request.getWithdrawalAccountNumber())
                .withdrawalBankName(request.getWithdrawalBankName())
                .applicationTime(LocalDateTime.now())
                .build();
    }

    private BigDecimal calculatePreferentialRate(Long userId) {
        // 멤버 프로필 조회
        return memberProfileRepository.findByMember_MemberId(userId)
                .map(profile -> {
                    // eco_level에 따른 우대금리 반환
                    MemberProfile.EcoLevel ecoLevel = profile.getEcoLevel();
                    if (ecoLevel == MemberProfile.EcoLevel.BEGINNER) {
                        return new BigDecimal("0.5");
                    } else if (ecoLevel == MemberProfile.EcoLevel.INTERMEDIATE) {
                        return new BigDecimal("1.0");
                    } else if (ecoLevel == MemberProfile.EcoLevel.EXPERT) {
                        return new BigDecimal("2.0");
                    } else {
                        return BigDecimal.ZERO;
                    }
                })
                .orElse(BigDecimal.ZERO); // 프로필이 없으면 0%
    }

    public static class SavingsApplicationResponse {
        private Long userId;
        private String userName;
        private String phoneNumber;
        private Long productId;
        private BigInteger applicationAmount;
        private BigDecimal preferentialRate;
        private Boolean autoTransferEnabled;
        private Integer transferDay;
        private Long monthlyTransferAmount;
        private String withdrawalAccountNumber;
        private String withdrawalBankName;
        private LocalDateTime applicationTime;

        public static SavingsApplicationResponseBuilder builder() {
            return new SavingsApplicationResponseBuilder();
        }

        public static class SavingsApplicationResponseBuilder {
            private Long userId;
            private String userName;
            private String phoneNumber;
            private Long productId;
            private BigInteger applicationAmount;
            private BigDecimal preferentialRate;
            private Boolean autoTransferEnabled;
            private Integer transferDay;
            private Long monthlyTransferAmount;
            private String withdrawalAccountNumber;
            private String withdrawalBankName;
            private LocalDateTime applicationTime;

            public SavingsApplicationResponseBuilder userId(Long userId) { this.userId = userId; return this; }
            public SavingsApplicationResponseBuilder userName(String userName) { this.userName = userName; return this; }
            public SavingsApplicationResponseBuilder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
            public SavingsApplicationResponseBuilder productId(Long productId) { this.productId = productId; return this; }
            public SavingsApplicationResponseBuilder applicationAmount(BigInteger applicationAmount) { this.applicationAmount = applicationAmount; return this; }
            public SavingsApplicationResponseBuilder preferentialRate(BigDecimal preferentialRate) { this.preferentialRate = preferentialRate; return this; }
            public SavingsApplicationResponseBuilder autoTransferEnabled(Boolean autoTransferEnabled) { this.autoTransferEnabled = autoTransferEnabled; return this; }
            public SavingsApplicationResponseBuilder transferDay(Integer transferDay) { this.transferDay = transferDay; return this; }
            public SavingsApplicationResponseBuilder monthlyTransferAmount(Long monthlyTransferAmount) { this.monthlyTransferAmount = monthlyTransferAmount; return this; }
            public SavingsApplicationResponseBuilder withdrawalAccountNumber(String withdrawalAccountNumber) { this.withdrawalAccountNumber = withdrawalAccountNumber; return this; }
            public SavingsApplicationResponseBuilder withdrawalBankName(String withdrawalBankName) { this.withdrawalBankName = withdrawalBankName; return this; }
            public SavingsApplicationResponseBuilder applicationTime(LocalDateTime applicationTime) { this.applicationTime = applicationTime; return this; }

            public SavingsApplicationResponse build() {
                SavingsApplicationResponse response = new SavingsApplicationResponse();
                response.userId = this.userId;
                response.userName = this.userName;
                response.phoneNumber = this.phoneNumber;
                response.productId = this.productId;
                response.applicationAmount = this.applicationAmount;
                response.preferentialRate = this.preferentialRate;
                response.autoTransferEnabled = this.autoTransferEnabled;
                response.transferDay = this.transferDay;
                response.monthlyTransferAmount = this.monthlyTransferAmount;
                response.withdrawalAccountNumber = this.withdrawalAccountNumber;
                response.withdrawalBankName = this.withdrawalBankName;
                response.applicationTime = this.applicationTime;
                return response;
            }
        }

        // Getters
        public Long getUserId() { return userId; }
        public String getUserName() { return userName; }
        public String getPhoneNumber() { return phoneNumber; }
        public Long getProductId() { return productId; }
        public BigInteger getApplicationAmount() { return applicationAmount; }
        public BigDecimal getPreferentialRate() { return preferentialRate; }
        public Boolean getAutoTransferEnabled() { return autoTransferEnabled; }
        public Integer getTransferDay() { return transferDay; }
        public Long getMonthlyTransferAmount() { return monthlyTransferAmount; }
        public String getWithdrawalAccountNumber() { return withdrawalAccountNumber; }
        public String getWithdrawalBankName() { return withdrawalBankName; }
        public LocalDateTime getApplicationTime() { return applicationTime; }
    }

    public static class CreateSavingsRequest {
        private Long savingProductId;
        private BigInteger applicationAmount;
        private String withdrawalAccountNumber;
        private String withdrawalBankName;
        
        // 자동이체 관련 필드들
        private Boolean autoTransferEnabled;
        private Integer transferDay;
        private Long monthlyTransferAmount;

        public CreateSavingsRequest(Long savingProductId, BigInteger applicationAmount,
                                   String withdrawalAccountNumber, String withdrawalBankName) {
            this.savingProductId = savingProductId;
            this.applicationAmount = applicationAmount;
            this.withdrawalAccountNumber = withdrawalAccountNumber;
            this.withdrawalBankName = withdrawalBankName;
        }

        // 자동이체 포함 생성자
        public CreateSavingsRequest(Long savingProductId, BigInteger applicationAmount,
                                   String withdrawalAccountNumber, String withdrawalBankName,
                                   Boolean autoTransferEnabled, Integer transferDay, Long monthlyTransferAmount) {
            this.savingProductId = savingProductId;
            this.applicationAmount = applicationAmount;
            this.withdrawalAccountNumber = withdrawalAccountNumber;
            this.withdrawalBankName = withdrawalBankName;
            this.autoTransferEnabled = autoTransferEnabled;
            this.transferDay = transferDay;
            this.monthlyTransferAmount = monthlyTransferAmount;
        }

        // Getters
        public Long getSavingProductId() { return savingProductId; }
        public BigInteger getApplicationAmount() { return applicationAmount; }
        public String getWithdrawalAccountNumber() { return withdrawalAccountNumber; }
        public String getWithdrawalBankName() { return withdrawalBankName; }
        public Boolean getAutoTransferEnabled() { return autoTransferEnabled; }
        public Integer getTransferDay() { return transferDay; }
        public Long getMonthlyTransferAmount() { return monthlyTransferAmount; }
    }
}