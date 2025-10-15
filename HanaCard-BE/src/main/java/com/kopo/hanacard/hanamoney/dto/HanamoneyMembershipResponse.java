package com.kopo.hanacard.hanamoney.dto;

import com.kopo.hanacard.hanamoney.domain.HanamoneyMembership;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class HanamoneyMembershipResponse {
    private Long id;
    private Long userId;
    private String membershipId;
    private Long balance;
    private Long totalEarned;
    private Long totalSpent;
    private String membershipLevel;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    public HanamoneyMembershipResponse(HanamoneyMembership membership) {
        this.id = membership.getId();
        this.userId = membership.getUserId();
        this.membershipId = membership.getMembershipId();
        this.balance = membership.getBalance();
        this.totalEarned = membership.getTotalEarned();
        this.totalSpent = membership.getTotalSpent();
        this.membershipLevel = membership.getMembershipLevel();
        this.isActive = membership.getIsActive();
        this.createdAt = membership.getCreatedAt();
        this.modifiedAt = membership.getUpdatedAt();
    }
}
