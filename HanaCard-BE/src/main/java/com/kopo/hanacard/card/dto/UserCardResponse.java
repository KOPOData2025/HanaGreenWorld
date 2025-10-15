package com.kopo.hanacard.card.dto;

import com.kopo.hanacard.card.domain.UserCard;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class UserCardResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long cardId;
    private String cardName;
    private String cardType;
    private String cardNumber;
    private String cardNumberMasked;
    private LocalDate expiryDate;
    private Long creditLimit;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserCardResponse(UserCard userCard) {
        this.id = userCard.getId();
        this.userId = userCard.getUser().getId();
        this.userName = userCard.getUser().getName();
        this.cardId = userCard.getCardProduct().getProductId();
        this.cardName = userCard.getCardProduct().getProductName();
        this.cardType = userCard.getCardProduct().getProductType();
        this.cardNumber = userCard.getCardNumber();
        this.cardNumberMasked = userCard.getCardNumberMasked();
        this.expiryDate = userCard.getExpiryDate();
        this.creditLimit = userCard.getCardProduct().getCreditLimit();
        this.isActive = userCard.getIsActive();
        this.createdAt = userCard.getCreatedAt();
        this.updatedAt = userCard.getUpdatedAt();
    }
}