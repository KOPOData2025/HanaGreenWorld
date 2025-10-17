package com.kopo.hanacard.card.dto;

import com.kopo.hanacard.card.domain.CardTransaction;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class CardTransactionResponse {
    private Long id;
    private LocalDateTime transactionDate;
    private String merchantName;
    private String category;
    private Long amount;
    private Long cashbackAmount;
    private BigDecimal cashbackRate;
    private String description;
    private String tags;

    public CardTransactionResponse(CardTransaction transaction) {
        this.id = transaction.getId();
        this.transactionDate = transaction.getTransactionDate();
        this.merchantName = transaction.getMerchantName();
        this.category = transaction.getCategory();
        this.amount = transaction.getAmount();
        this.cashbackAmount = transaction.getCashbackAmount();
        this.cashbackRate = transaction.getCashbackRate();
        this.description = transaction.getDescription();
        this.tags = transaction.getTags();
    }
}

