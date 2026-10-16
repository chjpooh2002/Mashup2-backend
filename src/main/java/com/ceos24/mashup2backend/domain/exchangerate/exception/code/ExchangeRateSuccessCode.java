package com.ceos24.mashup2backend.domain.exchangerate.exception.code;

import com.ceos24.mashup2backend.global.apipayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExchangeRateSuccessCode implements BaseSuccessCode {

    EXCHANGE_RATE_OK(HttpStatus.OK, "EXCHANGE-RATE200_1", "환율 조회에 성공하였습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

}
