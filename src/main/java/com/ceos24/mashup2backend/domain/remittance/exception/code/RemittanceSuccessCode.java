package com.ceos24.mashup2backend.domain.remittance.exception.code;

import com.ceos24.mashup2backend.global.apipayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RemittanceSuccessCode implements BaseSuccessCode {

    LATEST_REMITTANCE_OK(HttpStatus.OK, "REMITTANCE200_1", "송금 이력 조회에 성공하였습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

}
