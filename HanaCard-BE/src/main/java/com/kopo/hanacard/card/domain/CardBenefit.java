package com.kopo.hanacard.card.domain;

import com.kopo.hanacard.common.domain.DateTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "card_benefits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardBenefit extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private CardProduct cardProduct;

    @Column(name = "benefit_type", nullable = false)
    private String benefitType;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "cashback_rate", precision = 5, scale = 2)
    private BigDecimal cashbackRate;

    @Column(name = "discount_rate", precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(name = "description", columnDefinition = "CLOB")
    private String description;

    @Column(name = "min_amount")
    private Long minAmount;

    @Column(name = "max_amount")
    private Long maxAmount;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public CardBenefit(CardProduct cardProduct, String benefitType, String category,
                      BigDecimal cashbackRate, BigDecimal discountRate,
                      String description, Long minAmount, Long maxAmount) {
        this.cardProduct = cardProduct;
        this.benefitType = benefitType;
        this.category = category;
        this.cashbackRate = cashbackRate;
        this.discountRate = discountRate;
        this.description = description;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.isActive = true;
    }

    public void updateBenefitInfo(String description, BigDecimal cashbackRate, 
                                 BigDecimal discountRate, Long minAmount, Long maxAmount) {
        this.description = description;
        this.cashbackRate = cashbackRate;
        this.discountRate = discountRate;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public void deactivate() {
        this.isActive = false;
    }
}








