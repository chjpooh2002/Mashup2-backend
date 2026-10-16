package com.ceos24.mashup2backend.domain.exchangerate.service;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import com.ceos24.mashup2backend.domain.currency.repository.CurrencyRepository;
import com.ceos24.mashup2backend.domain.exchangerate.dto.request.ExchangeRateRequest;
import com.ceos24.mashup2backend.domain.exchangerate.dto.response.ExchangeRateResponse;
import com.ceos24.mashup2backend.domain.exchangerate.entity.ExchangeRate;
import com.ceos24.mashup2backend.domain.exchangerate.exception.ExchangeRateException;
import com.ceos24.mashup2backend.domain.exchangerate.exception.code.ExchangeRateErrorCode;
import com.ceos24.mashup2backend.domain.exchangerate.repository.ExchangeRateRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 로컬은 .env의 개발 DB를 그대로 쓰므로 테스트 트랜잭션 롤백으로 데이터를 남기지 않는다.
// 조회 서비스는 exchange_rate만 읽으므로 외부 환율 API는 호출되지 않는다
@SpringBootTest
@Transactional
class ExchangeRateQueryServiceIntegrationTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 10, 2, 10, 0);

    // 이슈 #19의 기준값. VND는 1원당 19.20, SGD는 1 SGD = 1051.92원의 역수
    private static final String VND_RATE = "19.2024656123";
    private static final String SGD_RATE = "0.0009506433";

    @Autowired
    private ExchangeRateQueryService exchangeRateQueryService;

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
    @DisplayName("1원당 값이 1 이상인 통화(VND)는 원화 기준으로 표시한다")
    void krwBasedDisplay() {
        saveRate("VND", VND_RATE);
        flushAndClear();

        ExchangeRateResponse result = exchangeRateQueryService.getExchangeRate(request("KRW", "VND"));

        assertThat(result.from()).isEqualTo("KRW");
        assertThat(result.to()).isEqualTo("VND");
        assertThat(result.rate()).isEqualTo(new BigDecimal("19.20"));
        assertThat(result.baseDate()).isEqualTo(BASE_TIME);
    }

    @Test
    @DisplayName("1원당 값이 1보다 작은 통화(SGD)는 표시 방향을 뒤집는다")
    void invertsDisplayDirectionForStrongCurrency() {
        saveRate("SGD", SGD_RATE);
        flushAndClear();

        ExchangeRateResponse result = exchangeRateQueryService.getExchangeRate(request("KRW", "SGD"));

        assertThat(result.from()).isEqualTo("SGD");
        assertThat(result.to()).isEqualTo("KRW");
        assertThat(result.rate()).isEqualTo(new BigDecimal("1051.92"));
        assertThat(result.baseDate()).isEqualTo(BASE_TIME);
    }

    @Test
    @DisplayName("보내는 통화가 KRW가 아니면 예외를 던진다")
    void throwsWhenSendCurrencyUnsupported() {
        saveRate("VND", VND_RATE);
        flushAndClear();

        assertThatThrownBy(() -> exchangeRateQueryService.getExchangeRate(request("USD", "VND")))
                .isInstanceOf(ExchangeRateException.class)
                .extracting(exception -> ((ExchangeRateException) exception).getErrorCode())
                .isEqualTo(ExchangeRateErrorCode.UNSUPPORTED_SEND_CURRENCY);
    }

    @Test
    @DisplayName("없는 받는 통화면 예외를 던진다")
    void throwsWhenCurrencyUnknown() {
        assertThatThrownBy(() -> exchangeRateQueryService.getExchangeRate(request("KRW", "XXX")))
                .isInstanceOf(ExchangeRateException.class)
                .extracting(exception -> ((ExchangeRateException) exception).getErrorCode())
                .isEqualTo(ExchangeRateErrorCode.RECEIVE_CURRENCY_NOT_FOUND);
    }

    @Test
    @DisplayName("비활성 통화도 없는 통화와 같은 예외로 막는다")
    void throwsWhenCurrencyInactive() {
        Currency inactive = saveInactiveCurrency();
        // 환율이 있어도 통화가 비활성이면 쓰지 않는다
        saveRate(inactive.getCurrencyCode(), "0.0237000000");
        flushAndClear();

        assertThatThrownBy(() -> exchangeRateQueryService.getExchangeRate(request("KRW", "THB")))
                .isInstanceOf(ExchangeRateException.class)
                .extracting(exception -> ((ExchangeRateException) exception).getErrorCode())
                .isEqualTo(ExchangeRateErrorCode.RECEIVE_CURRENCY_NOT_FOUND);
    }

    @Test
    @DisplayName("활성 통화인데 환율 행이 없으면 예외를 던진다")
    void throwsWhenRateMissing() {
        // KHR 환율은 넣지 않는다
        assertThatThrownBy(() -> exchangeRateQueryService.getExchangeRate(request("KRW", "KHR")))
                .isInstanceOf(ExchangeRateException.class)
                .extracting(exception -> ((ExchangeRateException) exception).getErrorCode())
                .isEqualTo(ExchangeRateErrorCode.EXCHANGE_RATE_NOT_FOUND);
    }

    private ExchangeRateRequest request(String sendCurrency, String receiveCurrency) {
        return new ExchangeRateRequest(sendCurrency, receiveCurrency);
    }

    private void saveRate(String currencyCode, String baseRate) {
        exchangeRateRepository.save(ExchangeRate.builder()
                .currency(currencyOf(currencyCode))
                .baseRate(new BigDecimal(baseRate))
                .baseTime(BASE_TIME)
                .updatedAt(LocalDateTime.now())
                .build());
    }

    // data.sql의 통화는 모두 활성이라 비활성 케이스는 직접 넣는다
    private Currency saveInactiveCurrency() {
        return currencyRepository.save(Currency.builder()
                .currencyCode("THB")
                .countryCode("TH")
                .countryName("태국")
                .decimalPlaces(2)
                .isActive(false)
                .build());
    }

    // 통화 초기 데이터는 data.sql이 넣는다
    private Currency currencyOf(String currencyCode) {
        return currencyRepository.findById(currencyCode).orElseThrow();
    }

    // DB 왕복을 거쳐 컬럼 scale이 반영된 값으로 읽게 한다
    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
