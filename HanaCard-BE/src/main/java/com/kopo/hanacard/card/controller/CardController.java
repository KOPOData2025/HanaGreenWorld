package com.kopo.hanacard.card.controller;

import com.kopo.hanacard.common.dto.ApiResponse;
import com.kopo.hanacard.card.domain.CardProduct;
import com.kopo.hanacard.card.domain.CardBenefit;
import com.kopo.hanacard.card.domain.UserCard;
import com.kopo.hanacard.card.dto.CardRegisterRequest;
import com.kopo.hanacard.card.dto.UserCardResponse;
import com.kopo.hanacard.card.dto.CardProductResponse;
import com.kopo.hanacard.card.service.CardService;
import com.kopo.hanacard.card.service.CardProductService;
import com.kopo.hanacard.card.service.CardTransactionService;
import com.kopo.hanacard.card.dto.CardTransactionResponse;
import com.kopo.hanacard.card.dto.CardConsumptionSummaryResponse;
import com.kopo.hanacard.card.dto.CardBenefitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "카드 관리", description = "카드 관련 API")
@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final CardProductService cardProductService;
    private final CardTransactionService cardTransactionService;

    @Operation(summary = "카드 목록 조회", description = "모든 활성 카드를 조회합니다.")
    @GetMapping
    public ApiResponse<List<CardProduct>> getAllCards() {
        List<CardProduct> cards = cardService.getAllCards();
        return ApiResponse.success(cards);
    }

    @Operation(summary = "카드 등록", description = "사용자에게 카드를 등록합니다.")
    @PostMapping("/register")
    public ApiResponse<UserCardResponse> registerCard(@Valid @RequestBody CardRegisterRequest request) {
        UserCard userCard = cardService.registerCard(
                request.getUserId(),
                request.getCardId()
        );
        return ApiResponse.success("카드가 성공적으로 등록되었습니다.", new UserCardResponse(userCard));
    }

    @Operation(summary = "사용자 카드 조회", description = "사용자의 모든 카드를 조회합니다.")
    @GetMapping("/user/{userId}")
    public ApiResponse<List<UserCardResponse>> getUserCards(@PathVariable Long userId) {
        List<UserCardResponse> responses = cardService.getUserCardResponses(userId);
        return ApiResponse.success(responses);
    }

    @Operation(summary = "카드번호로 조회", description = "카드번호로 카드를 조회합니다.")
    @GetMapping("/number/{cardNumber}")
    public ApiResponse<UserCardResponse> getUserCardByNumber(@PathVariable String cardNumber) {
        UserCard userCard = cardService.getUserCardByNumber(cardNumber);
        return ApiResponse.success(new UserCardResponse(userCard));
    }

    @Operation(summary = "카드 비활성화", description = "카드를 비활성화합니다.")
    @DeleteMapping("/{cardNumber}")
    public ApiResponse<Void> deactivateCard(@PathVariable String cardNumber) {
        cardService.deactivateCard(cardNumber);
        return ApiResponse.success("카드가 성공적으로 비활성화되었습니다.", null);
    }

    @Operation(summary = "카드 혜택 조회", description = "카드의 모든 혜택을 조회합니다.")
    @GetMapping("/{cardId}/benefits")
    public ApiResponse<List<CardBenefit>> getCardBenefits(@PathVariable Long cardId) {
        List<CardBenefit> benefits = cardService.getCardBenefits(cardId);
        return ApiResponse.success(benefits);
    }

    @Operation(summary = "카드 상품 목록 조회", description = "모든 활성 카드 상품을 조회합니다.")
    @GetMapping("/products")
    public ApiResponse<List<CardProductResponse>> getAllCardProducts() {
        List<CardProductResponse> products = cardProductService.getAllActiveCardProducts();
        return ApiResponse.success(products);
    }

    @Operation(summary = "카드 상품 타입별 조회", description = "카드 상품 타입별로 조회합니다.")
    @GetMapping("/products/type/{productType}")
    public ApiResponse<List<CardProductResponse>> getCardProductsByType(@PathVariable String productType) {
        List<CardProductResponse> products = cardProductService.getCardProductsByType(productType);
        return ApiResponse.success(products);
    }

    @Operation(summary = "사용자 카드 거래내역 조회", description = "사용자의 카드 거래내역을 조회합니다.")
    @GetMapping("/user/{userId}/transactions")
    public ApiResponse<List<CardTransactionResponse>> getUserCardTransactions(@PathVariable Long userId) {
        List<CardTransactionResponse> transactions = cardTransactionService.getUserCardTransactions(userId);
        return ApiResponse.success(transactions);
    }

    @Operation(summary = "월간 소비현황 요약", description = "사용자의 이번 달 카드 소비현황을 요약해서 조회합니다.")
    @GetMapping("/consumption/summary")
    public ApiResponse<CardConsumptionSummaryResponse> getMonthlyConsumptionSummary(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        CardConsumptionSummaryResponse summary = cardTransactionService.getMonthlyConsumptionSummary(userId);
        return ApiResponse.success(summary);
    }

    @Operation(summary = "카테고리별 거래내역 조회", description = "특정 카테고리의 거래내역을 조회합니다.")
    @GetMapping("/user/{userId}/transactions/category/{category}")
    public ApiResponse<List<CardTransactionResponse>> getTransactionsByCategory(@PathVariable Long userId, 
                                                                              @PathVariable String category) {
        List<CardTransactionResponse> transactions = cardTransactionService.getTransactionsByCategory(userId, category);
        return ApiResponse.success(transactions);
    }

    @Operation(summary = "사용자 카드 혜택 조회", description = "사용자 카드의 혜택을 조회합니다.")
    @GetMapping("/user/{userId}/benefits")
    public ApiResponse<List<CardBenefitResponse>> getUserCardBenefits(@PathVariable Long userId) {
        List<CardBenefit> benefits = cardService.getUserCardBenefits(userId);
        List<CardBenefitResponse> responses = benefits.stream()
                .map(CardBenefitResponse::new)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @Operation(summary = "친환경 소비현황 분석", description = "이번 달 친환경 소비현황을 분석합니다.")
    @GetMapping("/user/{userId}/eco-consumption")
    public ApiResponse<CardConsumptionSummaryResponse> getEcoConsumptionAnalysis(@PathVariable Long userId) {
        CardConsumptionSummaryResponse analysis = cardTransactionService.getEcoConsumptionAnalysis(userId);
        return ApiResponse.success(analysis);
    }

    @Operation(summary = "태그별 거래내역 조회", description = "특정 태그의 거래내역을 조회합니다.")
    @GetMapping("/user/{userId}/transactions/tag/{tag}")
    public ApiResponse<List<CardTransactionResponse>> getTransactionsByTag(@PathVariable Long userId, 
                                                                          @PathVariable String tag) {
        List<CardTransactionResponse> transactions = cardTransactionService.getTransactionsByTag(userId, tag);
        return ApiResponse.success(transactions);
    }
}

