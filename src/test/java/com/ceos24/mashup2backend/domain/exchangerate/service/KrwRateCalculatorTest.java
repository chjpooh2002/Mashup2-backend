package com.ceos24.mashup2backend.domain.exchangerate.service;

import com.ceos24.mashup2backend.domain.exchangerate.OpenExchangeRatesFixture;
import com.ceos24.mashup2backend.domain.exchangerate.client.dto.OpenExchangeRatesResponse;
import com.ceos24.mashup2backend.domain.exchangerate.exception.ExchangeRateException;
import com.ceos24.mashup2backend.domain.exchangerate.exception.code.ExchangeRateErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KrwRateCalculatorTest {

    private static final List<String> TARGETS = List.of("IDR", "VND", "SGD", "KHR");

    @Test
    @DisplayName("USD 기준 응답을 1원당 환율로 소수 10자리 HALF_UP 환산한다")
    void calculate() {
        KrwRates result = KrwRateCalculator.calculate(OpenExchangeRatesFixture.load(), TARGETS);

        assertThat(result.rates()).containsOnlyKeys(TARGETS);
        assertThat(result.rates().get("IDR")).isEqualTo(new BigDecimal("13.3129658895"));
        assertThat(result.rates().get("VND")).isEqualTo(new BigDecimal("19.2024656123"));
        assertThat(result.rates().get("SGD")).isEqualTo(new BigDecimal("0.0009506433"));
        assertThat(result.rates().get("KHR")).isEqualTo(new BigDecimal("3.0121824276"));
        // 서울 시간대 실행 환경 기준
        assertThat(result.baseTime()).isEqualTo(LocalDateTime.of(2026, 10, 15, 13, 0, 0));
    }

    @Test
    @DisplayName("응답에 KRW가 없으면 실패한다")
    void krwMissing() {
        assertErrorCode(OpenExchangeRatesFixture.without("KRW"), ExchangeRateErrorCode.KRW_RATE_MISSING);
    }

    @Test
    @DisplayName("KRW 값이 0이면 실패한다")
    void krwZero() {
        assertErrorCode(OpenExchangeRatesFixture.replacing("KRW", BigDecimal.ZERO),
                ExchangeRateErrorCode.NON_POSITIVE_RATE);
    }

    @Test
    @DisplayName("대상 통화 값이 음수면 실패한다")
    void targetNegative() {
        assertErrorCode(OpenExchangeRatesFixture.replacing("VND", new BigDecimal("-1")),
                ExchangeRateErrorCode.NON_POSITIVE_RATE);
    }

    @Test
    @DisplayName("대상 통화가 응답에 없으면 실패한다")
    void targetMissing() {
        assertErrorCode(OpenExchangeRatesFixture.without("SGD"), ExchangeRateErrorCode.TARGET_RATE_MISSING);
    }

    private void assertErrorCode(OpenExchangeRatesResponse response, ExchangeRateErrorCode errorCode) {
        assertThatThrownBy(() -> KrwRateCalculator.calculate(response, TARGETS))
                .isInstanceOf(ExchangeRateException.class)
                .extracting(e -> ((ExchangeRateException) e).getErrorCode())
                .isEqualTo(errorCode);
    }
}
