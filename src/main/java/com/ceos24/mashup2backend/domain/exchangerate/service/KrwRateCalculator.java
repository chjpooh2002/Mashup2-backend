package com.ceos24.mashup2backend.domain.exchangerate.service;

import com.ceos24.mashup2backend.domain.exchangerate.client.dto.OpenExchangeRatesResponse;
import com.ceos24.mashup2backend.domain.exchangerate.exception.ExchangeRateException;
import com.ceos24.mashup2backend.domain.exchangerate.exception.code.ExchangeRateErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// USD 기준 환율 응답을 1원당 환율로 환산한다
public final class KrwRateCalculator {

    public static final String KRW = "KRW";

    // 매매기준율 컬럼 scale(20, 10)과 맞춘다
    private static final int RATE_SCALE = 10;

    private KrwRateCalculator() {
    }

    /***
     * 함수 기능: 응답을 검증하고 대상 통화별 1원당 환율을 계산한다.
     * 1원당 X = (USD당 X) ÷ (USD당 KRW)
     * @param response Open Exchange Rates 응답 (USD 기준)
     * @param targetCodes 갱신 대상 통화 코드 목록
     * @return KrwRates (통화별 1원당 환율, 고시 시각)
     */
    public static KrwRates calculate(OpenExchangeRatesResponse response, List<String> targetCodes) {
        if (response.timestamp() == null) {
            throw new ExchangeRateException(ExchangeRateErrorCode.TIMESTAMP_MISSING);
        }

        Map<String, BigDecimal> usdRates = response.rates() == null ? Map.of() : response.rates();
        BigDecimal usdKrw = requirePositive(usdRates.get(KRW), ExchangeRateErrorCode.KRW_RATE_MISSING);

        // 대상 통화가 하나라도 빠지거나 이상하면 전체를 버린다 (일부만 갱신하지 않는다)
        Map<String, BigDecimal> krwRates = new LinkedHashMap<>();
        for (String code : targetCodes) {
            BigDecimal usdRate = requirePositive(usdRates.get(code), ExchangeRateErrorCode.TARGET_RATE_MISSING);
            krwRates.put(code, usdRate.divide(usdKrw, RATE_SCALE, RoundingMode.HALF_UP));
        }

        // 타임존은 코드에 박지 않고 시스템 기본값(컨테이너 TZ)을 따른다
        LocalDateTime baseTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(response.timestamp()), ZoneId.systemDefault());

        return new KrwRates(krwRates, baseTime);
    }

    // 값이 없으면 missingCode, 0 이하면 NON_POSITIVE_RATE
    private static BigDecimal requirePositive(BigDecimal rate, ExchangeRateErrorCode missingCode) {
        if (rate == null) {
            throw new ExchangeRateException(missingCode);
        }
        if (rate.signum() <= 0) {
            throw new ExchangeRateException(ExchangeRateErrorCode.NON_POSITIVE_RATE);
        }
        return rate;
    }
}
