package com.ceos24.mashup2backend.domain.remittance.exception.code;

import com.ceos24.mashup2backend.global.apipayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RemittanceErrorCode implements BaseErrorCode {

    // 없는 통화와 비활성 통화를 같은 에러로 처리한다 (receive-options와 같은 정책)
    RECEIVE_CURRENCY_NOT_FOUND(HttpStatus.NOT_FOUND, "REMITTANCE404_1",
            "지원하지 않는 받는 통화입니다."),

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
