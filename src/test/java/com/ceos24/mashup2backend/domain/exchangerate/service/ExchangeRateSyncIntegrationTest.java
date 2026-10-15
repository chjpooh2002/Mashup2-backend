package com.ceos24.mashup2backend.domain.exchangerate.service;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import com.ceos24.mashup2backend.domain.currency.repository.CurrencyRepository;
import com.ceos24.mashup2backend.domain.exchangerate.OpenExchangeRatesFixture;
import com.ceos24.mashup2backend.domain.exchangerate.client.OpenExchangeRatesClient;
import com.ceos24.mashup2backend.domain.exchangerate.client.dto.OpenExchangeRatesResponse;
import com.ceos24.mashup2backend.domain.exchangerate.entity.ExchangeRate;
import com.ceos24.mashup2backend.domain.exchangerate.repository.ExchangeRateRepository;
import com.ceos24.mashup2backend.domain.exchangerate.scheduler.ExchangeRateScheduler;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 로컬은 .env의 개발 DB를 그대로 쓰므로 테스트 트랜잭션 롤백으로 데이터를 남기지 않는다
// 외부 API는 클라이언트를 mock으로 대체해 실제로 호출하지 않는다
@SpringBootTest
@Transactional
class ExchangeRateSyncIntegrationTest {

    private static final LocalDateTime OLD_TIME = LocalDateTime.of(2020, 1, 1, 0, 0);

    @MockitoBean
    private OpenExchangeRatesClient openExchangeRatesClient;

    @Autowired
    private ExchangeRateSyncService exchangeRateSyncService;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        exchangeRateRepository.deleteAllInBatch();
        given(openExchangeRatesClient.hasAppId()).willReturn(true);
    }

    @Test
    @DisplayName("환율 행이 없으면 활성 통화마다 새로 생성한다")
    void createWhenAbsent() {
        given(openExchangeRatesClient.fetchLatest(anyCollection())).willReturn(responseForActiveCurrencies());

        exchangeRateSyncService.sync();
        flushAndClear();

        assertThat(rateOf("IDR").getBaseRate()).isEqualTo(new BigDecimal("13.3129658895"));
        assertThat(rateOf("VND").getBaseRate()).isEqualTo(new BigDecimal("19.2024656123"));
        assertThat(rateOf("SGD").getBaseRate()).isEqualTo(new BigDecimal("0.0009506433"));
        assertThat(rateOf("KHR").getBaseRate()).isEqualTo(new BigDecimal("3.0121824276"));
        // 서울 시간대 실행 환경 기준
        assertThat(rateOf("VND").getBaseTime()).isEqualTo(LocalDateTime.of(2026, 10, 15, 13, 0, 0));
        assertThat(exchangeRateRepository.count()).isEqualTo(currencyRepository.findAllByIsActiveTrue().size());
    }

    @Test
    @DisplayName("환율 행이 있으면 값과 시각을 갱신한다")
    void updateWhenPresent() {
        saveOldRate("VND");
        given(openExchangeRatesClient.fetchLatest(anyCollection())).willReturn(responseForActiveCurrencies());

        exchangeRateSyncService.sync();
        flushAndClear();

        ExchangeRate vnd = rateOf("VND");
        assertThat(vnd.getBaseRate()).isEqualTo(new BigDecimal("19.2024656123"));
        // 서울 시간대 실행 환경 기준
        assertThat(vnd.getBaseTime()).isEqualTo(LocalDateTime.of(2026, 10, 15, 13, 0, 0));
        assertThat(vnd.getUpdatedAt()).isAfter(OLD_TIME);
    }

    @Test
    @DisplayName("응답이 이상하면 기존 값을 덮어쓰지 않는다")
    void keepWhenInvalidResponse() {
        saveOldRate("VND");
        given(openExchangeRatesClient.fetchLatest(anyCollection()))
                .willReturn(OpenExchangeRatesFixture.without("KRW"));

        exchangeRateSyncService.sync();
        flushAndClear();

        assertOldRateKept("VND");
    }

    @Test
    @DisplayName("앱 키가 비어 있으면 클라이언트를 호출하지 않는다")
    void skipWhenAppIdMissing() {
        saveOldRate("VND");
        given(openExchangeRatesClient.hasAppId()).willReturn(false);

        exchangeRateSyncService.sync();
        flushAndClear();

        verify(openExchangeRatesClient, never()).fetchLatest(anyCollection());
        assertOldRateKept("VND");
    }

    @Test
    @DisplayName("prod가 아닌 프로필에는 스케줄러 빈이 없다")
    void noSchedulerOutsideProd() {
        assertThat(applicationContext.getBeansOfType(ExchangeRateScheduler.class)).isEmpty();
    }

    // 실제 응답 + 개발 DB에 data.sql 외 활성 통화가 더 있으면 그 통화 값도 채운 응답
    private OpenExchangeRatesResponse responseForActiveCurrencies() {
        Map<String, BigDecimal> rates = new HashMap<>(OpenExchangeRatesFixture.load().rates());
        currencyRepository.findAllByIsActiveTrue().stream()
                .map(Currency::getCurrencyCode)
                .forEach(code -> rates.putIfAbsent(code, BigDecimal.ONE));
        return OpenExchangeRatesFixture.withRates(rates);
    }

    private void saveOldRate(String code) {
        exchangeRateRepository.save(ExchangeRate.builder()
                .currency(currencyRepository.getReferenceById(code))
                .baseRate(BigDecimal.ONE)
                .baseTime(OLD_TIME)
                .updatedAt(OLD_TIME)
                .build());
        flushAndClear();
    }

    private void assertOldRateKept(String code) {
        ExchangeRate rate = rateOf(code);
        assertThat(rate.getBaseRate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(rate.getBaseTime()).isEqualTo(OLD_TIME);
        assertThat(rate.getUpdatedAt()).isEqualTo(OLD_TIME);
    }

    private ExchangeRate rateOf(String code) {
        return exchangeRateRepository.findById(code).orElseThrow();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
