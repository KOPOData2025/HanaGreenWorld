package com.kopo.hanacard.card.dto;

import com.kopo.hanacard.card.domain.CardBenefit;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class CardBenefitResponse {
    private Long id;
    private String benefitType;
    private String category;
    private String description;
    private BigDecimal cashbackRate;
    private BigDecimal discountRate;
    private Long minAmount;
    private Long maxAmount;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CardBenefitResponse(CardBenefit benefit) {
        this.id = benefit.getId();
        this.benefitType = benefit.getBenefitType();
        this.category = benefit.getCategory();
        this.description = benefit.getDescription();
        this.cashbackRate = benefit.getCashbackRate();
        this.discountRate = benefit.getDiscountRate();
        this.minAmount = benefit.getMinAmount();
        this.maxAmount = benefit.getMaxAmount();
        this.isActive = benefit.getIsActive();
        this.createdAt = benefit.getCreatedAt();
        this.updatedAt = benefit.getUpdatedAt();
    }
}
