package com.ceos24.mashup2backend.domain.exchangerate.exception;

import com.ceos24.mashup2backend.global.apipayload.code.BaseErrorCode;
import com.ceos24.mashup2backend.global.apipayload.exception.ProjectException;

public class ExchangeRateException extends ProjectException {
    public ExchangeRateException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
