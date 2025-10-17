package com.kopo.hanacard.card.domain;

import com.kopo.hanacard.common.domain.DateTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "card_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardProduct extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "product_type", nullable = false, length = 50)
    private String productType;

    @Column(name = "description", columnDefinition = "CLOB")
    private String description;

    @Column(name = "annual_fee", nullable = false)
    private Long annualFee;

    @Column(name = "credit_limit")
    private Long creditLimit;

    @Column(name = "benefits", columnDefinition = "CLOB")
    private String benefits;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public CardProduct(String productName, String productType, String description, 
                      Long annualFee, Long creditLimit, String benefits, String imageUrl) {
        this.productName = productName;
        this.productType = productType;
        this.description = description;
        this.annualFee = annualFee;
        this.creditLimit = creditLimit;
        this.benefits = benefits;
        this.imageUrl = imageUrl;
        this.isActive = true;
        // DateTimeEntity 필드들은 @PrePersist에서 자동 설정되지만, Oracle에서는 명시적 설정이 필요할 수 있음
    }

    public void updateProduct(String productName, String productType, String description,
                             Long annualFee, Long creditLimit, String benefits, String imageUrl) {
        this.productName = productName;
        this.productType = productType;
        this.description = description;
        this.annualFee = annualFee;
        this.creditLimit = creditLimit;
        this.benefits = benefits;
        this.imageUrl = imageUrl;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
