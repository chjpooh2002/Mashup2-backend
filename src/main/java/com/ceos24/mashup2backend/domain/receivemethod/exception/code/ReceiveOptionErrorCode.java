package com.ceos24.mashup2backend.domain.receivemethod.exception.code;

import com.ceos24.mashup2backend.global.apipayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReceiveOptionErrorCode implements BaseErrorCode {

    UNSUPPORTED_SEND_CURRENCY(HttpStatus.BAD_REQUEST, "RECEIVE-OPTION400_1", "보내는 통화는 KRW만 지원합니다."),
    RECEIVE_CURRENCY_NOT_FOUND(HttpStatus.NOT_FOUND, "RECEIVE-OPTION404_1", "지원하지 않는 받는 통화입니다."),
    EXCHANGE_RATE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "RECEIVE-OPTION500_1", "해당 통화의 환율 정보가 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

}
