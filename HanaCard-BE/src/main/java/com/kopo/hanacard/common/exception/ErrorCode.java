package com.kopo.hanacard.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 사용자 관련
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS("U002", "이미 존재하는 사용자입니다."),
    INVALID_USER_INFO("U003", "유효하지 않은 사용자 정보입니다."),
    
    // 카드 관련
    CARD_NOT_FOUND("C001", "카드를 찾을 수 없습니다."),
    CARD_ALREADY_EXISTS("C002", "이미 존재하는 카드입니다."),
    INVALID_CARD_INFO("C003", "유효하지 않은 카드 정보입니다."),
    CARD_BENEFIT_NOT_FOUND("C004", "카드 혜택을 찾을 수 없습니다."),
    USER_CARD_NOT_FOUND("C005", "사용자 카드를 찾을 수 없습니다."),
    TRANSACTION_CREATE_FAILED("C006", "카드 거래 내역을 저장할 수 없습니다."),
    
    // 하나머니 관련
    HANAMONEY_ACCOUNT_NOT_FOUND("H001", "하나머니 계좌를 찾을 수 없습니다."),
    INSUFFICIENT_HANAMONEY("H002", "하나머니 잔액이 부족합니다."),
    INVALID_HANAMONEY_AMOUNT("H003", "유효하지 않은 하나머니 금액입니다."),
    HANAMONEY_TRANSACTION_FAILED("H004", "하나머니 거래가 실패했습니다."),
    INSUFFICIENT_BALANCE("H005", "잔액이 부족합니다."),
    
    // 공통
    INTERNAL_SERVER_ERROR("E001", "서버 내부 오류가 발생했습니다."),
    INVALID_REQUEST("E002", "유효하지 않은 요청입니다."),
    UNAUTHORIZED("E003", "인증이 필요합니다."),
    FORBIDDEN("E004", "접근 권한이 없습니다."),
    NOT_FOUND("E005", "요청한 리소스를 찾을 수 없습니다."),
    ALREADY_EXISTS("E006", "이미 존재하는 리소스입니다.");

    private final String code;
    private final String message;
}

