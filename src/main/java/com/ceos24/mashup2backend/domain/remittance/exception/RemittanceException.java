package com.ceos24.mashup2backend.domain.remittance.exception;

import com.ceos24.mashup2backend.global.apipayload.code.BaseErrorCode;
import com.ceos24.mashup2backend.global.apipayload.exception.ProjectException;

public class RemittanceException extends ProjectException {

    public RemittanceException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
