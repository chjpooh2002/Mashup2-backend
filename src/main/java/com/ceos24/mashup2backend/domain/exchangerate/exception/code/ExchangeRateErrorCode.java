package com.ceos24.mashup2backend.domain.exchangerate.exception.code;

import com.ceos24.mashup2backend.global.apipayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExchangeRateErrorCode implements BaseErrorCode {

    RATES_API_CALL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EXCHANGE-RATE500_1", "환율 API 호출에 실패했습니다."),
    TIMESTAMP_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "EXCHANGE-RATE500_2", "환율 API 응답에 고시 시각이 없습니다."),
    KRW_RATE_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "EXCHANGE-RATE500_3", "환율 API 응답에 원화(KRW) 환율이 없습니다."),
    NON_POSITIVE_RATE(HttpStatus.INTERNAL_SERVER_ERROR, "EXCHANGE-RATE500_4", "환율 API 응답에 0 이하의 환율이 있습니다."),
    TARGET_RATE_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "EXCHANGE-RATE500_5", "환율 API 응답에 갱신 대상 통화의 환율이 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

}
