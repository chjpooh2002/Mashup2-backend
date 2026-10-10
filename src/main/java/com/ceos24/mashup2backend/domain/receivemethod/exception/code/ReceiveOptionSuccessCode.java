package com.ceos24.mashup2backend.domain.receivemethod.exception.code;

import com.ceos24.mashup2backend.global.apipayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReceiveOptionSuccessCode implements BaseSuccessCode {

    RECEIVE_OPTION_LIST_OK(HttpStatus.OK, "RECEIVE-OPTION200_1", "받는 방법 리스트 조회에 성공하였습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

}
