package com.ceos24.mashup2backend.domain.exchangerate;

import com.ceos24.mashup2backend.domain.exchangerate.client.dto.OpenExchangeRatesResponse;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

// src/test/resources/exchangerate/latest-response.json (실제 응답) 로더
public final class OpenExchangeRatesFixture {

    private static final String PATH = "/exchangerate/latest-response.json";
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private OpenExchangeRatesFixture() {
    }

    public static OpenExchangeRatesResponse load() {
        try (InputStream in = OpenExchangeRatesFixture.class.getResourceAsStream(PATH)) {
            return JSON_MAPPER.readValue(in, OpenExchangeRatesResponse.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // 실제 응답의 rates를 바꾼 사본
    public static OpenExchangeRatesResponse withRates(Map<String, BigDecimal> rates) {
        OpenExchangeRatesResponse response = load();
        return new OpenExchangeRatesResponse(response.timestamp(), response.base(), rates);
    }

    // 실제 응답의 rates에서 통화 하나를 뺀 사본
    public static OpenExchangeRatesResponse without(String code) {
        Map<String, BigDecimal> rates = new HashMap<>(load().rates());
        rates.remove(code);
        return withRates(rates);
    }

    // 실제 응답의 rates에서 통화 하나의 값을 바꾼 사본
    public static OpenExchangeRatesResponse replacing(String code, BigDecimal rate) {
        Map<String, BigDecimal> rates = new HashMap<>(load().rates());
        rates.put(code, rate);
        return withRates(rates);
    }
}
