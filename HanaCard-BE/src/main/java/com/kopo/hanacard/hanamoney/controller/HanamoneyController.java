package com.kopo.hanacard.hanamoney.controller;

import com.kopo.hanacard.common.dto.ApiResponse;
import com.kopo.hanacard.hanamoney.domain.HanamoneyMembership;
import com.kopo.hanacard.hanamoney.domain.HanamoneyTransaction;
import com.kopo.hanacard.hanamoney.dto.HanamoneyMembershipResponse;
import com.kopo.hanacard.hanamoney.dto.HanamoneyTransactionRequest;
import com.kopo.hanacard.hanamoney.dto.HanamoneyTransactionResponse;
import com.kopo.hanacard.hanamoney.service.HanamoneyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "하나머니 멤버십 관리", description = "하나머니 통합 멤버십 서비스 관련 API")
@RestController
@RequestMapping("/hanamoney")
@RequiredArgsConstructor
public class HanamoneyController {

    private final HanamoneyService hanamoneyService;

    @Operation(summary = "하나머니 멤버십 가입", description = "사용자에게 하나머니 멤버십을 가입시킵니다.")
    @PostMapping("/memberships")
    public ApiResponse<HanamoneyMembershipResponse> createHanamoneyMembership(@RequestParam Long userId) {
        HanamoneyMembership membership = hanamoneyService.createHanamoneyMembership(userId);
        return ApiResponse.success("하나머니 멤버십이 성공적으로 가입되었습니다.", new HanamoneyMembershipResponse(membership));
    }

    @Operation(summary = "하나머니 멤버십 조회", description = "사용자 ID로 하나머니 멤버십을 조회합니다.")
    @GetMapping("/memberships/user/{userId}")
    public ApiResponse<HanamoneyMembershipResponse> getHanamoneyMembershipByUserId(@PathVariable Long userId) {
        HanamoneyMembership membership = hanamoneyService.getHanamoneyMembershipByUserId(userId);
        return ApiResponse.success(new HanamoneyMembershipResponse(membership));
    }

    @Operation(summary = "하나머니 멤버십 ID로 조회", description = "멤버십 ID로 하나머니 멤버십을 조회합니다.")
    @GetMapping("/memberships/{membershipId}")
    public ApiResponse<HanamoneyMembershipResponse> getHanamoneyMembershipById(@PathVariable Long membershipId) {
        HanamoneyMembership membership = hanamoneyService.getHanamoneyMembershipById(membershipId);
        return ApiResponse.success(new HanamoneyMembershipResponse(membership));
    }

    @Operation(summary = "하나머니 적립", description = "금융거래를 통해 하나머니를 적립합니다.")
    @PostMapping("/earn")
    public ApiResponse<HanamoneyMembershipResponse> earn(@Valid @RequestBody HanamoneyTransactionRequest request) {
        HanamoneyMembership membership = hanamoneyService.earn(
                request.getUserId(),
                request.getAmount(),
                request.getDescription()
        );
        return ApiResponse.success("하나머니가 성공적으로 적립되었습니다.", new HanamoneyMembershipResponse(membership));
    }

    @Operation(summary = "하나머니 사용", description = "하나머니를 사용합니다 (결제, ATM 출금 등).")
    @PostMapping("/spend")
    public ApiResponse<HanamoneyMembershipResponse> spend(@Valid @RequestBody HanamoneyTransactionRequest request) {
        HanamoneyMembership membership = hanamoneyService.spend(
                request.getUserId(),
                request.getAmount(),
                request.getDescription()
        );
        return ApiResponse.success("하나머니가 성공적으로 사용되었습니다.", new HanamoneyMembershipResponse(membership));
    }

    @Operation(summary = "ATM 출금", description = "하나머니를 현금으로 출금합니다.")
    @PostMapping("/atm-withdraw")
    public ApiResponse<HanamoneyMembershipResponse> atmWithdraw(@Valid @RequestBody HanamoneyTransactionRequest request) {
        HanamoneyMembership membership = hanamoneyService.atmWithdraw(
                request.getUserId(),
                request.getAmount(),
                request.getDescription()
        );
        return ApiResponse.success("ATM 출금이 성공적으로 처리되었습니다.", new HanamoneyMembershipResponse(membership));
    }

    @Operation(summary = "제휴사 포인트 교환", description = "하나머니를 제휴사 포인트로 교환합니다.")
    @PostMapping("/exchange-partner")
    public ApiResponse<HanamoneyMembershipResponse> exchangeToPartner(@RequestParam Long userId,
                                                                     @RequestParam Long amount,
                                                                     @RequestParam String partnerName,
                                                                     @RequestParam(required = false) String description) {
        HanamoneyMembership membership = hanamoneyService.exchangeToPartner(userId, amount, partnerName, description);
        return ApiResponse.success("제휴사 포인트 교환이 성공적으로 처리되었습니다.", new HanamoneyMembershipResponse(membership));
    }

    @Operation(summary = "하나머니 이체", description = "사용자 간 하나머니를 이체합니다.")
    @PostMapping("/transfer")
    public ApiResponse<HanamoneyMembershipResponse> transfer(@RequestParam Long fromUserId,
                                                           @RequestParam Long toUserId,
                                                           @RequestParam Long amount,
                                                           @RequestParam(required = false) String description) {
        HanamoneyMembership membership = hanamoneyService.transferTo(fromUserId, toUserId, amount, description);
        return ApiResponse.success("이체가 성공적으로 처리되었습니다.", new HanamoneyMembershipResponse(membership));
    }

    @Operation(summary = "거래 내역 조회", description = "하나머니 거래 내역을 조회합니다.")
    @GetMapping("/transactions/user/{userId}")
    public ApiResponse<List<HanamoneyTransactionResponse>> getTransactionHistory(@PathVariable Long userId,
                                                                                @RequestParam(required = false) LocalDateTime startDate,
                                                                                @RequestParam(required = false) LocalDateTime endDate) {
        List<HanamoneyTransaction> transactions;
        
        if (startDate != null && endDate != null) {
            transactions = hanamoneyService.getTransactionHistoryByDateRange(userId, startDate, endDate);
        } else {
            // 기본적으로 최근 30일
            LocalDateTime defaultStartDate = LocalDateTime.now().minusDays(30);
            LocalDateTime defaultEndDate = LocalDateTime.now();
            transactions = hanamoneyService.getTransactionHistoryByDateRange(userId, defaultStartDate, defaultEndDate);
        }
        
        List<HanamoneyTransactionResponse> responses = transactions.stream()
                .map(HanamoneyTransactionResponse::new)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @Operation(summary = "거래 타입별 내역 조회", description = "거래 타입별로 거래 내역을 조회합니다.")
    @GetMapping("/transactions/user/{userId}/type/{transactionType}")
    public ApiResponse<List<HanamoneyTransactionResponse>> getTransactionHistoryByType(@PathVariable Long userId,
                                                                                       @PathVariable String transactionType) {
        HanamoneyTransaction.TransactionType type = HanamoneyTransaction.TransactionType.valueOf(transactionType.toUpperCase());
        List<HanamoneyTransaction> transactions = hanamoneyService.getTransactionHistoryByType(userId, type);
        
        List<HanamoneyTransactionResponse> responses = transactions.stream()
                .map(HanamoneyTransactionResponse::new)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }
}

