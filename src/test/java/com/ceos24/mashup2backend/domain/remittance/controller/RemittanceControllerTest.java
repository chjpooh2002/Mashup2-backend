package com.ceos24.mashup2backend.domain.remittance.controller;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import com.ceos24.mashup2backend.domain.currency.repository.CurrencyRepository;
import com.ceos24.mashup2backend.domain.exchangerate.entity.ExchangeRate;
import com.ceos24.mashup2backend.domain.exchangerate.repository.ExchangeRateRepository;
import com.ceos24.mashup2backend.domain.remittance.entity.RecentRemittance;
import com.ceos24.mashup2backend.domain.remittance.repository.RecentRemittanceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 직렬화 형태와 상태 코드를 검증한다 — BigDecimal scale과 LocalDateTime 포맷이 응답에 그대로 드러나고,
// 저장 API는 200이 아니라 201을 내려야 한다
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RemittanceControllerTest {

    private static final String URL = "/api/v1/remittances";
    private static final String LATEST_URL = URL + "/latest";
    private static final Long FIXED_USER_ID = 1L;
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 10, 2, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecentRemittanceRepository recentRemittanceRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void setUp() {
        recentRemittanceRepository.deleteAllInBatch();
        exchangeRateRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("이력이 없으면 기본값 응답을 공통 형식으로 내려준다")
    void defaultResponseShape() throws Exception {
        saveRate("VND", "19.0300000000");
        flushAndClear();

        mockMvc.perform(get(LATEST_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("REMITTANCE200_1"))
                .andExpect(jsonPath("$.message").value("송금 이력 조회에 성공하였습니다."))
                .andExpect(jsonPath("$.result.hasHistory").value(false))
                .andExpect(jsonPath("$.result.send.countryCode").value("KR"))
                .andExpect(jsonPath("$.result.receive.currency").value("VND"))
                .andExpect(jsonPath("$.result.exchangeRate.from").value("KRW"))
                .andExpect(jsonPath("$.result.exchangeRate.to").value("VND"))
                // 고시 시각은 ISO-8601 기본 형식으로 나간다
                .andExpect(jsonPath("$.result.exchangeRate.baseDate").value("2026-10-02T10:00:00"))
                // scale이 그대로 드러나지 않는지 본다 (19.0300000000, 10000.0000이 아님)
                .andExpect(content().string(containsString("\"rate\":19.03,")))
                .andExpect(content().string(containsString("\"amount\":10000}")))
                .andExpect(content().string(containsString("\"amount\":190300}")));
    }

    @Test
    @DisplayName("SGD 이력은 방향이 뒤집힌 환율로 직렬화된다")
    void invertedDirectionResponseShape() throws Exception {
        saveRate("VND", "19.0300000000");
        saveRate("SGD", "0.0009440553");
        saveRemittance("SGD", 1_000_000L, LocalDateTime.of(2026, 10, 1, 12, 0));
        flushAndClear();

        mockMvc.perform(get(LATEST_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.hasHistory").value(true))
                .andExpect(jsonPath("$.result.exchangeRate.from").value("SGD"))
                .andExpect(jsonPath("$.result.exchangeRate.to").value("KRW"))
                .andExpect(content().string(containsString("\"rate\":1059.26,")))
                // 원화 금액은 1000000.0000이 아니라 1000000으로 나간다
                .andExpect(content().string(containsString("\"amount\":1000000}")))
                .andExpect(content().string(containsString("\"amount\":944.06}")));
    }

    @Test
    @DisplayName("저장 성공은 200이 아니라 201로 내려간다")
    void saveReturnsCreated() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiveCurrency\":\"VND\",\"sendAmount\":1000000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("REMITTANCE201_1"))
                .andExpect(jsonPath("$.message").value("송금 이력 저장에 성공하였습니다."))
                .andExpect(jsonPath("$.result.remittanceId").isNumber())
                .andExpect(jsonPath("$.result.receiveCurrency").value("VND"))
                .andExpect(jsonPath("$.result.sentAt").isNotEmpty())
                // 요청 금액을 원화 자릿수로 맞춰 내린다 (1000000.0000이 아님)
                .andExpect(content().string(containsString("\"sendAmount\":1000000,")));
    }

    @Test
    @DisplayName("저장한 뒤 조회하면 방금 보낸 나라와 금액이 내려온다")
    void savedRemittanceIsReadBack() throws Exception {
        saveRate("VND", "19.0300000000");
        saveRate("SGD", "0.0009440553");
        flushAndClear();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiveCurrency\":\"SGD\",\"sendAmount\":1000000}"))
                .andExpect(status().isCreated());
        flushAndClear();

        mockMvc.perform(get(LATEST_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.hasHistory").value(true))
                .andExpect(jsonPath("$.result.receive.currency").value("SGD"))
                .andExpect(jsonPath("$.result.send.amount").value(1000000));
    }

    @Test
    @DisplayName("금액이 0 이하면 400과 필드별 메시지를 내려준다")
    void rejectsNonPositiveAmount() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiveCurrency\":\"VND\",\"sendAmount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON400_1"))
                .andExpect(jsonPath("$.result.sendAmount").value("보내는 금액은 0보다 커야 합니다."));
    }

    @Test
    @DisplayName("받는 통화가 비어 있으면 400과 필드별 메시지를 내려준다")
    void rejectsBlankCurrency() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sendAmount\":1000000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"))
                .andExpect(jsonPath("$.result.receiveCurrency").value("받는 통화는 필수입니다."));
    }

    @Test
    @DisplayName("지원하지 않는 통화면 404를 내려준다")
    void rejectsUnknownCurrency() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiveCurrency\":\"XXX\",\"sendAmount\":1000000}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("REMITTANCE404_1"));
    }

    private void saveRate(String currencyCode, String baseRate) {
        exchangeRateRepository.save(ExchangeRate.builder()
                .currency(currencyOf(currencyCode))
                .baseRate(new BigDecimal(baseRate))
                .baseTime(BASE_TIME)
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private void saveRemittance(String currencyCode, long sendAmount, LocalDateTime sentAt) {
        recentRemittanceRepository.save(RecentRemittance.builder()
                .currency(currencyOf(currencyCode))
                .userId(FIXED_USER_ID)
                .sendAmount(BigDecimal.valueOf(sendAmount))
                .sentAt(sentAt)
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
