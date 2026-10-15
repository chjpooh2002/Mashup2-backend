package com.ceos24.mashup2backend.domain.exchangerate.client.dto;

import java.math.BigDecimal;
import java.util.Map;

// Open Exchange Rates latest.json 응답. 필요한 필드만 받는다
public record OpenExchangeRatesResponse(

        // 고시 시각. 초 단위 유닉스 시간
        Long timestamp,

        // 기준 통화. 무료 플랜은 USD 고정
        String base,

        // 기준 통화 1단위당 각 통화 값. 정수(VND)도 오므로 double을 거치지 않도록 BigDecimal로 받는다
        Map<String, BigDecimal> rates

) {
}
