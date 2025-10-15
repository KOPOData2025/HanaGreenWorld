package com.kopo.hanacard.hanamoney.domain;

import com.kopo.hanacard.common.domain.DateTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hanamoney_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HanamoneyTransaction extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false)
    private HanamoneyMembership membership;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(name = "description")
    private String description;

    @Builder
    public HanamoneyTransaction(HanamoneyMembership membership, TransactionType transactionType,
                               Long amount, Long balanceAfter, String description) {
        this.membership = membership;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    public enum TransactionType {
        EARN("적립"),
        SPEND("사용"),
        TRANSFER_IN("이체입금"),
        TRANSFER_OUT("이체출금"),
        ATM_WITHDRAWAL("ATM출금"),
        PARTNER_EXCHANGE("제휴사교환"),
        CASHBACK("캐시백"),
        REFUND("환불"),
        PAYMENT("결제"),
        CARD_PAYMENT("카드결제"),
        POINT_CONVERSION("포인트전환");

        private final String description;

        TransactionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Getter method for DTOs
    public HanamoneyMembership getAccount() {
        return this.membership;
    }
}

