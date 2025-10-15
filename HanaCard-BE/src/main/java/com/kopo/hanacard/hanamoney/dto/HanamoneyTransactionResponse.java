package com.kopo.hanacard.hanamoney.dto;

import com.kopo.hanacard.hanamoney.domain.HanamoneyTransaction;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class HanamoneyTransactionResponse {
    private Long id;
    private Long accountId;
    private String transactionType;
    private String transactionTypeDescription;
    private Long amount;
    private Long balanceAfter;
    private String description;
    private LocalDateTime createdAt;

    public HanamoneyTransactionResponse(HanamoneyTransaction transaction) {
        this.id = transaction.getId();
        this.accountId = transaction.getAccount().getId();
        this.transactionType = transaction.getTransactionType().name();
        this.transactionTypeDescription = transaction.getTransactionType().getDescription();
        this.amount = transaction.getAmount();
        this.balanceAfter = transaction.getBalanceAfter();
        this.description = transaction.getDescription();
        this.createdAt = transaction.getCreatedAt();
    }
}
















