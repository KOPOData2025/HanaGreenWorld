package com.kopo.hanacard.hanamoney.domain;

import com.kopo.hanacard.common.domain.DateTimeEntity;
import com.kopo.hanacard.user.domain.User;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hanamoney_memberships")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HanamoneyMembership extends DateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hanamoney_memberships_seq")
    @SequenceGenerator(name = "hanamoney_memberships_seq", sequenceName = "HANAMONEY_MEMBERSHIPS_SEQ", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "membership_id", nullable = false, unique = true)
    private String membershipId;

    @Column(name = "balance", nullable = false)
    private Long balance = 0L;

    @Column(name = "total_earned", nullable = false)
    private Long totalEarned = 0L;

    @Column(name = "total_spent", nullable = false)
    private Long totalSpent = 0L;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "membership_level", nullable = false)
    @Setter(AccessLevel.PRIVATE)
    private String membershipLevel = "BASIC";

    @Builder
    public HanamoneyMembership(User user, String membershipId, Long balance, Long totalEarned, Long totalSpent, Boolean isActive, String membershipLevel) {
        this.user = user;
        this.membershipId = membershipId;
        this.balance = balance != null ? balance : 0L;
        this.totalEarned = totalEarned != null ? totalEarned : 0L;
        this.totalSpent = totalSpent != null ? totalSpent : 0L;
        this.isActive = isActive != null ? isActive : true;
        this.membershipLevel = membershipLevel != null ? membershipLevel : "BASIC";
    }

    // 하나머니 적립 (금융거래를 통해)
    public void earn(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("적립 금액은 0보다 커야 합니다.");
        }
        this.balance += amount;
        this.totalEarned += amount;
        updateMembershipLevel();
    }

    // 하나머니 사용 (결제, ATM 출금 등)
    public void spend(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }
        if (this.balance < amount) {
            throw new IllegalArgumentException("하나머니 잔액이 부족합니다.");
        }
        this.balance -= amount;
        this.totalSpent += amount;
    }

    // 하나머니 이체 (다른 사용자에게)
    public void transferTo(HanamoneyMembership targetMembership, Long amount) {
        this.spend(amount);
        targetMembership.earn(amount);
    }

    // ATM 출금 (현금으로)
    public void withdrawToCash(Long amount) {
        this.spend(amount);
    }

    // 제휴사 포인트 교환
    public void exchangeToPartnerPoints(Long amount, String partnerName) {
        this.spend(amount);
    }

    // 멤버십 레벨 업데이트
    private void updateMembershipLevel() {
        if (this.totalEarned >= 1000000) {
            this.membershipLevel = "GOLD";
        } else if (this.totalEarned >= 500000) {
            this.membershipLevel = "SILVER";
        } else {
            this.membershipLevel = "BASIC";
        }
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    // Getter methods for DTOs
    public Long getUserId() {
        return this.user.getId();
    }

    public String getMembershipId() {
        return this.membershipId;
    }

    public String getMembershipLevel() {
        return this.membershipLevel;
    }

    public Boolean isActive() { return this.isActive; }
}

