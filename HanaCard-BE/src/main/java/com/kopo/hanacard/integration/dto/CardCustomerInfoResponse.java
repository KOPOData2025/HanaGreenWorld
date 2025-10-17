package com.kopo.hanacard.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardCustomerInfoResponse {

    private CustomerBasicInfo customerInfo;

    private List<CardInfo> cards;

    private HanamoneyInfo hanamoneyInfo;

    private LocalDateTime responseTime;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerBasicInfo {
        private String name;
        private String email;
        private String phoneNumber;
        private String customerGrade; // DIAMOND, PLATINUM, GOLD 등
        private LocalDateTime joinDate;
        private boolean isActive;
        private BigDecimal totalCreditLimit; // 총 신용한도
        private BigDecimal usedCreditAmount; // 사용한 신용금액
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardInfo {
        private String cardNumber; // 마스킹된 카드번호
        private String cardName;
        private String cardType; // CREDIT, DEBIT, PREPAID
        private String cardStatus; // ACTIVE, INACTIVE, SUSPENDED
        private BigDecimal creditLimit;
        private BigDecimal availableLimit;
        private LocalDateTime issueDate;
        private LocalDateTime expiryDate;
        private List<String> benefits; // 카드 혜택 목록
        private BigDecimal monthlyUsage; // 이번 달 사용금액
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HanamoneyInfo {
        private boolean isSubscribed; // 하나머니 가입 여부
        private String membershipLevel; // BASIC, PREMIUM, VIP
        private BigDecimal currentPoints; // 현재 포인트
        private BigDecimal accumulatedPoints; // 누적 포인트
        private LocalDateTime joinDate;
        private BigDecimal monthlyEarnings; // 이번 달 적립 포인트
        private List<String> availableBenefits; // 사용 가능한 혜택
    }
}














