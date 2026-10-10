package com.ceos24.mashup2backend.domain.receivemethod.exception;

import com.ceos24.mashup2backend.global.apipayload.code.BaseErrorCode;
import com.ceos24.mashup2backend.global.apipayload.exception.ProjectException;

public class ReceiveOptionException extends ProjectException {
    public ReceiveOptionException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
