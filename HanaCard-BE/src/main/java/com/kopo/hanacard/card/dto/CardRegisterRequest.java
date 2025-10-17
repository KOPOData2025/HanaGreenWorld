package com.kopo.hanacard.card.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CardRegisterRequest {

    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @NotNull(message = "카드 ID는 필수입니다.")
    private Long cardId;

    private String currentBenefitType;
}













