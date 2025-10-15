package com.kopo.hanacard.card.domain;

import com.kopo.hanacard.common.domain.DateTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardTransaction extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_card_id", nullable = false)
    private UserCard userCard;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "merchant_name", nullable = false, length = 200)
    private String merchantName;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "cashback_amount")
    private Long cashbackAmount = 0L;

    @Column(name = "cashback_rate", precision = 5, scale = 2)
    private BigDecimal cashbackRate = BigDecimal.ZERO;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "merchant_category", length = 50)
    private String merchantCategory; // EV_CHARGING, PUBLIC_TRANSPORT 등

    @Column(name = "tags", length = 500)
    private String tags; // 친환경, 대중교통, 전기차 등 태그들을 쉼표로 구분

    @Column(name = "business_number", length = 20)
    private String businessNumber; // 사업자 번호 (친환경 가맹점 매칭용)

    @Builder
    public CardTransaction(UserCard userCard, LocalDateTime transactionDate, String merchantName,
                          String category, Long amount, Long cashbackAmount, BigDecimal cashbackRate,
                          String description, String merchantCategory, String tags, String businessNumber) {
        this.userCard = userCard;
        this.transactionDate = transactionDate;
        this.merchantName = merchantName;
        this.category = category;
        this.amount = amount;
        this.cashbackAmount = cashbackAmount != null ? cashbackAmount : 0L;
        this.cashbackRate = cashbackRate != null ? cashbackRate : BigDecimal.ZERO;
        this.description = description;
        this.merchantCategory = merchantCategory;
        this.tags = tags;
        this.businessNumber = businessNumber;
    }
}

