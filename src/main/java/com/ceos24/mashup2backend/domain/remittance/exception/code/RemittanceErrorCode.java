package com.ceos24.mashup2backend.domain.remittance.exception.code;

import com.ceos24.mashup2backend.global.apipayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RemittanceErrorCode implements BaseErrorCode {

    // 이력이 없거나 쓸 수 없으면 기본 통화(VND)로 폴백하는데, 그 기본값마저 없으면 더 이상 폴백할 곳이 없다
    DEFAULT_CURRENCY_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "REMITTANCE500_1",
            "기본 받는 통화 정보가 없습니다."),
    DEFAULT_EXCHANGE_RATE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "REMITTANCE500_2",
            "기본 받는 통화의 환율 정보가 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

}
