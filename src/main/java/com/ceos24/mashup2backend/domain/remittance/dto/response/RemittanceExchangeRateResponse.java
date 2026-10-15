package com.ceos24.mashup2backend.domain.remittance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 환율 블록. from/to는 화면에 보여줄 방향이라 통화에 따라 뒤집힌다
public record RemittanceExchangeRateResponse(

        // 기준 통화. 원화 기준 표시면 KRW, 뒤집혔으면 받는 통화 (ex. SGD)
        String from,

        // 대상 통화. 원화 기준 표시면 받는 통화, 뒤집혔으면 KRW
        String to,

        // from 1단위당 to 금액. 소수점 2자리
        BigDecimal rate,

        // 환율 고시 시각
        LocalDateTime baseDate

) {
}
