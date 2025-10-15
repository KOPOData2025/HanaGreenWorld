package com.kopo.hanacard.card.domain;

import com.kopo.hanacard.common.domain.DateTimeEntity;
import com.kopo.hanacard.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "user_cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCard extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_product_id", nullable = false)
    private CardProduct cardProduct;

    @Column(name = "card_number", nullable = false, unique = true)
    private String cardNumber;

    @Column(name = "card_number_masked", nullable = false)
    private String cardNumberMasked;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "cvv", nullable = false)
    private String cvv;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public UserCard(User user, CardProduct cardProduct, String cardNumber, String cardNumberMasked,
                   LocalDate expiryDate, String cvv, Boolean isActive) {
        this.user = user;
        this.cardProduct = cardProduct;
        this.cardNumber = cardNumber;
        this.cardNumberMasked = cardNumberMasked;
        this.expiryDate = expiryDate;
        this.cvv = cvv;
        this.isActive = isActive != null ? isActive : true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public CardProduct getCardProduct() {
        return this.cardProduct;
    }
}




