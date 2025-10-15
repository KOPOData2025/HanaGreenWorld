package com.kopo.hanacard.card.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class CardConsumptionSummaryResponse {
    private Long totalAmount;
    private Long totalCashback;
    private Long ecoAmount;
    private Long ecoCashback;
    private Double ecoRatio;
    private Map<String, Long> categoryAmounts;
    private Map<String, Long> ecoCategoryAmounts;
    private List<CardTransactionResponse> recentTransactions;

    public CardConsumptionSummaryResponse(Long totalAmount, Long totalCashback, 
                                        Map<String, Long> categoryAmounts,
                                        List<CardTransactionResponse> recentTransactions) {
        this.totalAmount = totalAmount;
        this.totalCashback = totalCashback;
        this.categoryAmounts = categoryAmounts;
        this.recentTransactions = recentTransactions;
    }

    public CardConsumptionSummaryResponse(Long totalAmount, Long totalCashback, Long ecoAmount, Long ecoCashback, 
                                        Double ecoRatio, Map<String, Long> categoryAmounts, 
                                        Map<String, Long> ecoCategoryAmounts, List<CardTransactionResponse> recentTransactions) {
        this.totalAmount = totalAmount;
        this.totalCashback = totalCashback;
        this.ecoAmount = ecoAmount;
        this.ecoCashback = ecoCashback;
        this.ecoRatio = ecoRatio;
        this.categoryAmounts = categoryAmounts;
        this.ecoCategoryAmounts = ecoCategoryAmounts;
        this.recentTransactions = recentTransactions;
    }
}

