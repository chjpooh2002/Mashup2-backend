package com.ceos24.mashup2backend.domain.exchangerate.controller;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import com.ceos24.mashup2backend.domain.currency.repository.CurrencyRepository;
import com.ceos24.mashup2backend.domain.exchangerate.entity.ExchangeRate;
import com.ceos24.mashup2backend.domain.exchangerate.repository.ExchangeRateRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 직렬화 형태를 검증한다 — BigDecimal scale과 LocalDateTime 포맷이 응답에 그대로 드러나고,
// /api/v1/remittances/latest의 exchangeRate 블록과 같은 모양이어야 한다
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExchangeRateControllerTest {

    private static final String URL = "/api/v1/exchange-rates";
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 10, 2, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void setUp() {
        exchangeRateRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("환율을 공통 형식으로 내려준다")
    void responseShape() throws Exception {
        saveRate("VND", "19.2024656123");
        flushAndClear();

        mockMvc.perform(get(URL)
                        .param("sendCurrency", "KRW")
                        .param("receiveCurrency", "VND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("EXCHANGE-RATE200_1"))
                .andExpect(jsonPath("$.message").value("환율 조회에 성공하였습니다."))
                .andExpect(jsonPath("$.result.from").value("KRW"))
                .andExpect(jsonPath("$.result.to").value("VND"))
                // 고시 시각은 ISO-8601 기본 형식으로 나간다
                .andExpect(jsonPath("$.result.baseDate").value("2026-10-02T10:00:00"))
                // 소수점 2자리로 나간다 (19.2024656123이 아님)
                .andExpect(content().string(containsString("\"rate\":19.20,")));
    }

    @Test
    @DisplayName("받는 통화가 없으면 400과 파라미터별 메시지를 내려준다")
    void rejectsMissingReceiveCurrency() throws Exception {
        mockMvc.perform(get(URL)
                        .param("sendCurrency", "KRW"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON400_1"))
                .andExpect(jsonPath("$.result.receiveCurrency").value("받는 통화는 필수입니다."));
    }

    @Test
    @DisplayName("보내는 통화가 없으면 400과 파라미터별 메시지를 내려준다")
    void rejectsMissingSendCurrency() throws Exception {
        mockMvc.perform(get(URL)
                        .param("receiveCurrency", "VND"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"))
                .andExpect(jsonPath("$.result.sendCurrency").value("보내는 통화는 필수입니다."));
    }

    private void saveRate(String currencyCode, String baseRate) {
        exchangeRateRepository.save(ExchangeRate.builder()
                .currency(currencyOf(currencyCode))
                .baseRate(new BigDecimal(baseRate))
                .baseTime(BASE_TIME)
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private Currency currencyOf(String currencyCode) {
        return currencyRepository.findById(currencyCode).orElseThrow();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
