package com.kopo.hanagreenworld.integration.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
public class LoanCalculationUtil {

    public static BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualInterestRate, int loanTermMonths) {
        if (principal == null || annualInterestRate == null || loanTermMonths <= 0) {
            log.warn("대출 상환금 계산 파라미터 오류 - principal: {}, interestRate: {}, termMonths: {}", 
                    principal, annualInterestRate, loanTermMonths);
            return BigDecimal.ZERO;
        }

        if (annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            // 무이자 대출인 경우
            return principal.divide(BigDecimal.valueOf(loanTermMonths), 0, RoundingMode.HALF_UP);
        }

        try {
            // 월 이자율 계산
            BigDecimal monthlyRate = annualInterestRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
            
            // (1 + r)^n 계산
            BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
            BigDecimal powerTerm = onePlusRate.pow(loanTermMonths);
            
            // 월 상환금 계산: P * r * (1 + r)^n / ((1 + r)^n - 1)
            BigDecimal numerator = principal.multiply(monthlyRate).multiply(powerTerm);
            BigDecimal denominator = powerTerm.subtract(BigDecimal.ONE);
            
            BigDecimal monthlyPayment = numerator.divide(denominator, 0, RoundingMode.HALF_UP);
            
            log.debug("대출 상환금 계산 완료 - 원금: {}, 연이자율: {}%, 기간: {}개월, 월상환금: {}", 
                    principal, annualInterestRate.multiply(BigDecimal.valueOf(100)), loanTermMonths, monthlyPayment);
            
            return monthlyPayment;
            
        } catch (Exception e) {
            log.error("대출 상환금 계산 중 오류 발생: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 대출 시작일과 만기일을 이용하여 대출 기간(월)을 계산합니다
     * 
     * @param startDate 대출 시작일
     * @param maturityDate 만기일
     * @return 대출 기간 (월)
     */
    public static int calculateLoanTermMonths(LocalDateTime startDate, LocalDateTime maturityDate) {
        if (startDate == null || maturityDate == null) {
            log.warn("대출 기간 계산 파라미터 오류 - startDate: {}, maturityDate: {}", startDate, maturityDate);
            return 36; // 기본값 3년
        }

        if (maturityDate.isBefore(startDate)) {
            log.warn("만기일이 시작일보다 이전입니다 - startDate: {}, maturityDate: {}", startDate, maturityDate);
            return 36; // 기본값 3년
        }

        long months = ChronoUnit.MONTHS.between(startDate, maturityDate);
        return Math.max(1, (int) months); // 최소 1개월
    }

    /**
     * 대출 정보를 이용하여 월 상환금을 계산합니다
     * 
     * @param loanAmount 대출금액
     * @param interestRate 이자율
     * @param startDate 대출 시작일
     * @param maturityDate 만기일
     * @return 월 상환금
     */
    public static BigDecimal calculateMonthlyPaymentFromLoanInfo(BigDecimal loanAmount, BigDecimal interestRate, 
                                                               LocalDateTime startDate, LocalDateTime maturityDate) {
        if (loanAmount == null || interestRate == null) {
            log.warn("대출 정보가 부족합니다 - loanAmount: {}, interestRate: {}", loanAmount, interestRate);
            return BigDecimal.ZERO;
        }

        int loanTermMonths = calculateLoanTermMonths(startDate, maturityDate);
        return calculateMonthlyPayment(loanAmount, interestRate, loanTermMonths);
    }

    public static int calculateRemainingMonths(LocalDateTime maturityDate) {
        if (maturityDate == null) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        if (maturityDate.isBefore(now)) {
            return 0; // 이미 만료된 대출
        }

        long months = ChronoUnit.MONTHS.between(now, maturityDate);
        return Math.max(0, (int) months);
    }

    public static BigDecimal calculateMonthlyInterest(BigDecimal remainingAmount, BigDecimal annualInterestRate) {
        if (remainingAmount == null || annualInterestRate == null) {
            log.warn("월 이자 계산 파라미터 오류 - remainingAmount: {}, interestRate: {}", remainingAmount, annualInterestRate);
            return BigDecimal.ZERO;
        }

        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO; // 잔여금액이 0 이하면 이자 없음
        }

        try {
            // 월 이자율 계산
            BigDecimal monthlyRate = annualInterestRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
            
            // 월 이자 = 잔여금액 × 월이자율
            BigDecimal monthlyInterest = remainingAmount.multiply(monthlyRate);
            
            log.debug("월 이자 계산 완료 - 잔여금액: {}, 연이자율: {}%, 월이자: {}", 
                    remainingAmount, annualInterestRate.multiply(BigDecimal.valueOf(100)), monthlyInterest);
            
            return monthlyInterest.setScale(0, RoundingMode.HALF_UP);
            
        } catch (Exception e) {
            log.error("월 이자 계산 중 오류 발생: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    public static LoanPaymentInfo calculateLoanPaymentInfo(BigDecimal loanAmount, BigDecimal remainingAmount, 
                                                          BigDecimal interestRate, LocalDateTime startDate, 
                                                          LocalDateTime maturityDate) {
        if (loanAmount == null || interestRate == null) {
            log.warn("대출 정보가 부족합니다 - loanAmount: {}, interestRate: {}", loanAmount, interestRate);
            return new LoanPaymentInfo(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // 월 상환금 계산 (원금)
        BigDecimal monthlyPayment = calculateMonthlyPaymentFromLoanInfo(loanAmount, interestRate, startDate, maturityDate);
        
        // 월 이자 계산 (잔여금액 기준)
        BigDecimal actualRemainingAmount = remainingAmount != null ? remainingAmount : loanAmount;
        BigDecimal monthlyInterest = calculateMonthlyInterest(actualRemainingAmount, interestRate.divide(new BigDecimal("100")));
        
        // 총 월 납입금 = 월 상환금 + 월 이자
        BigDecimal totalMonthlyPayment = monthlyPayment.add(monthlyInterest);
        
        return new LoanPaymentInfo(monthlyPayment, monthlyInterest, totalMonthlyPayment);
    }

    public static class LoanPaymentInfo {
        private final BigDecimal monthlyPayment; // 월 상환금 (원금)
        private final BigDecimal monthlyInterest; // 월 이자
        private final BigDecimal totalMonthlyPayment; // 총 월 납입금

        public LoanPaymentInfo(BigDecimal monthlyPayment, BigDecimal monthlyInterest, BigDecimal totalMonthlyPayment) {
            this.monthlyPayment = monthlyPayment;
            this.monthlyInterest = monthlyInterest;
            this.totalMonthlyPayment = totalMonthlyPayment;
        }

        public BigDecimal getMonthlyPayment() {
            return monthlyPayment;
        }

        public BigDecimal getMonthlyInterest() {
            return monthlyInterest;
        }

        public BigDecimal getTotalMonthlyPayment() {
            return totalMonthlyPayment;
        }
    }
}
