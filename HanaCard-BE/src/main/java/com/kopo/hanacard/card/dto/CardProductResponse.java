package com.kopo.hanacard.card.dto;

import com.kopo.hanacard.card.domain.CardProduct;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CardProductResponse {
    private Long productId;
    private String productName;
    private String productType;
    private String description;
    private Long annualFee;
    private Long creditLimit;
    private String benefits;
    private String imageUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    public CardProductResponse(CardProduct cardProduct) {
        this.productId = cardProduct.getProductId();
        this.productName = cardProduct.getProductName();
        this.productType = cardProduct.getProductType();
        this.description = cardProduct.getDescription();
        this.annualFee = cardProduct.getAnnualFee();
        this.creditLimit = cardProduct.getCreditLimit();
        this.benefits = cardProduct.getBenefits();
        this.imageUrl = cardProduct.getImageUrl();
        this.isActive = cardProduct.getIsActive();
        this.createdAt = cardProduct.getCreatedAt();
        this.modifiedAt = cardProduct.getUpdatedAt();
    }
}
