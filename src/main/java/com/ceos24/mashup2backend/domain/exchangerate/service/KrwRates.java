package com.ceos24.mashup2backend.domain.exchangerate.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

// 검증·환산을 마친 저장 대상 환율 묶음
public record KrwRates(

        // 통화 코드 → 1원당 받는 통화 값
        Map<String, BigDecimal> rates,

        // 고시 시각
        LocalDateTime baseTime

) {
}
