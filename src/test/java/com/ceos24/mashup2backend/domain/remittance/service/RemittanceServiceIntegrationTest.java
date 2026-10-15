package com.ceos24.mashup2backend.domain.remittance.service;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import com.ceos24.mashup2backend.domain.currency.repository.CurrencyRepository;
import com.ceos24.mashup2backend.domain.exchangerate.entity.ExchangeRate;
import com.ceos24.mashup2backend.domain.exchangerate.repository.ExchangeRateRepository;
import com.ceos24.mashup2backend.domain.remittance.dto.request.RemittanceSaveRequest;
import com.ceos24.mashup2backend.domain.remittance.dto.response.LatestRemittanceResponse;
import com.ceos24.mashup2backend.domain.remittance.dto.response.RemittanceSaveResponse;
import com.ceos24.mashup2backend.domain.remittance.entity.RecentRemittance;
import com.ceos24.mashup2backend.domain.remittance.exception.RemittanceException;
import com.ceos24.mashup2backend.domain.remittance.exception.code.RemittanceErrorCode;
import com.ceos24.mashup2backend.domain.remittance.repository.RecentRemittanceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

// 로컬은 .env의 개발 DB를 그대로 쓰므로 테스트 트랜잭션 롤백으로 데이터를 남기지 않는다
@SpringBootTest
@Transactional
class RemittanceServiceIntegrationTest {

    private static final Long FIXED_USER_ID = 1L;
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 10, 2, 10, 0);

    // 이슈 #15의 기준값. VND는 1원당 19.03, SGD는 1 SGD = 1059.26원의 역수
    private static final String VND_RATE = "19.0300000000";
    private static final String SGD_RATE = "0.0009440553";

    @Autowired
    private RemittanceService remittanceService;

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
    @DisplayName("이력이 없으면 KRW 10,000 -> VND 기본값으로 응답한다")
    void defaultWhenNoHistory() {
        saveRate("VND", VND_RATE);
        flushAndClear();

        LatestRemittanceResponse result = remittanceService.getLatestRemittance();

        assertThat(result.hasHistory()).isFalse();
        assertThat(result.send().countryCode()).isEqualTo("KR");
        assertThat(result.send().currency()).isEqualTo("KRW");
        assertThat(result.send().amount()).isEqualTo(new BigDecimal("10000"));
        assertThat(result.receive().countryCode()).isEqualTo("VN");
        assertThat(result.receive().currency()).isEqualTo("VND");
        // VND는 decimalPlaces가 0이다
        assertThat(result.receive().amount()).isEqualTo(new BigDecimal("190300"));
        assertThat(result.exchangeRate().from()).isEqualTo("KRW");
        assertThat(result.exchangeRate().to()).isEqualTo("VND");
        assertThat(result.exchangeRate().rate()).isEqualTo(new BigDecimal("19.03"));
        assertThat(result.exchangeRate().baseDate()).isEqualTo(BASE_TIME);
    }

    @Test
    @DisplayName("1원당 값이 1보다 작은 통화(SGD)는 표시 방향을 뒤집는다")
    void invertsDisplayDirectionForStrongCurrency() {
        saveRate("VND", VND_RATE);
        saveRate("SGD", SGD_RATE);
        saveRemittance("SGD", 1_000_000L, LocalDateTime.of(2026, 10, 1, 12, 0));
        flushAndClear();

        LatestRemittanceResponse result = remittanceService.getLatestRemittance();

        assertThat(result.hasHistory()).isTrue();
        // 컬럼 scale이 4라도 원화 자릿수(0)로 맞춰 내려간다
        assertThat(result.send().amount()).isEqualTo(new BigDecimal("1000000"));
        assertThat(result.receive().countryCode()).isEqualTo("SG");
        assertThat(result.receive().currency()).isEqualTo("SGD");
        // 표시 환율(1059.26)이 아니라 baseRate로 환산한다
        assertThat(result.receive().amount()).isEqualTo(new BigDecimal("944.06"));
        assertThat(result.exchangeRate().from()).isEqualTo("SGD");
        assertThat(result.exchangeRate().to()).isEqualTo("KRW");
        assertThat(result.exchangeRate().rate()).isEqualTo(new BigDecimal("1059.26"));
    }

    @Test
    @DisplayName("이력이 여러 건이면 sentAt이 가장 늦은 1건을 쓴다")
    void usesMostRecentHistory() {
        saveRate("VND", VND_RATE);
        saveRate("SGD", SGD_RATE);
        saveRate("IDR", "13.3129658895");
        saveRemittance("SGD", 1_000_000L, LocalDateTime.of(2026, 10, 1, 12, 0));
        saveRemittance("IDR", 500_000L, LocalDateTime.of(2026, 10, 3, 9, 0));
        saveRemittance("VND", 300_000L, LocalDateTime.of(2026, 10, 2, 8, 0));
        flushAndClear();

        LatestRemittanceResponse result = remittanceService.getLatestRemittance();

        assertThat(result.hasHistory()).isTrue();
        assertThat(result.receive().currency()).isEqualTo("IDR");
        assertThat(result.send().amount()).isEqualTo(new BigDecimal("500000"));
        // IDR은 1원당 값이 1보다 커서 원화 기준으로 표시한다
        assertThat(result.exchangeRate().from()).isEqualTo("KRW");
        assertThat(result.exchangeRate().to()).isEqualTo("IDR");
        assertThat(result.exchangeRate().rate()).isEqualTo(new BigDecimal("13.31"));
    }

    @Test
    @DisplayName("이력의 받는 통화가 비활성이면 기본값으로 폴백한다")
    void fallsBackWhenHistoryCurrencyInactive() {
        Currency inactive = saveInactiveCurrency();
        saveRate("VND", VND_RATE);
        // 환율이 있어도 통화가 비활성이면 쓰지 않는다
        saveRate(inactive.getCurrencyCode(), "0.0237000000");
        saveRemittance("THB", 1_000_000L, LocalDateTime.of(2026, 10, 1, 12, 0));
        flushAndClear();

        LatestRemittanceResponse result = remittanceService.getLatestRemittance();

        assertThat(result.hasHistory()).isFalse();
        assertThat(result.receive().currency()).isEqualTo("VND");
        assertThat(result.send().amount()).isEqualTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("이력의 받는 통화에 환율이 없으면 기본값으로 폴백한다")
    void fallsBackWhenHistoryRateMissing() {
        saveRate("VND", VND_RATE);
        // KHR 환율은 넣지 않는다
        saveRemittance("KHR", 1_000_000L, LocalDateTime.of(2026, 10, 1, 12, 0));
        flushAndClear();

        LatestRemittanceResponse result = remittanceService.getLatestRemittance();

        assertThat(result.hasHistory()).isFalse();
        assertThat(result.receive().currency()).isEqualTo("VND");
    }

    @Test
    @DisplayName("기본 통화의 환율까지 없으면 폴백할 곳이 없어 예외를 던진다")
    void throwsWhenDefaultRateMissing() {
        saveRemittance("KHR", 1_000_000L, LocalDateTime.of(2026, 10, 1, 12, 0));
        flushAndClear();

        assertThatThrownBy(() -> remittanceService.getLatestRemittance())
                .isInstanceOf(RemittanceException.class)
                .extracting(exception -> ((RemittanceException) exception).getErrorCode())
                .isEqualTo(RemittanceErrorCode.DEFAULT_EXCHANGE_RATE_NOT_FOUND);
    }

    @Test
    @DisplayName("폼 제출 내용을 유저 1의 이력으로 저장한다")
    void savesRemittance() {
        RemittanceSaveResponse result = remittanceService.saveRemittance(
                new RemittanceSaveRequest("VND", BigDecimal.valueOf(1_000_000)));
        flushAndClear();

        assertThat(result.remittanceId()).isNotNull();
        assertThat(result.receiveCurrency()).isEqualTo("VND");
        // 컬럼 scale이 4라도 원화 자릿수(0)로 맞춰 내려간다
        assertThat(result.sendAmount()).isEqualTo(new BigDecimal("1000000"));
        assertThat(result.sentAt()).isNotNull();

        RecentRemittance saved = recentRemittanceRepository.findById(result.remittanceId()).orElseThrow();
        assertThat(saved.getUserId()).isEqualTo(FIXED_USER_ID);
        assertThat(saved.getCurrency().getCurrencyCode()).isEqualTo("VND");
        assertThat(saved.getSendAmount()).isEqualByComparingTo("1000000");
        // 공통 Auditing이 없어 서비스가 직접 채운다.
        // 초 미만 자리는 정확히 비교하지 않는다 — LocalDateTime.now()는 100ns 단위까지 주는데
        // DATETIME(6)에 저장될 때 마이크로초로 반올림되어 응답으로 돌려준 값과 끝자리가 달라진다
        assertThat(saved.getSentAt()).isCloseTo(result.sentAt(), within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("같은 요청을 두 번 보내면 행이 두 개 쌓인다")
    void accumulatesRows() {
        RemittanceSaveRequest request = new RemittanceSaveRequest("VND", BigDecimal.valueOf(1_000_000));

        remittanceService.saveRemittance(request);
        remittanceService.saveRemittance(request);
        flushAndClear();

        List<RecentRemittance> saved = recentRemittanceRepository.findAll();
        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(remittance -> remittance.getCurrency().getCurrencyCode())
                .containsExactly("VND", "VND");
    }

    @Test
    @DisplayName("저장한 이력을 곧바로 조회로 읽을 수 있다")
    void savedRemittanceIsReadBack() {
        saveRate("SGD", SGD_RATE);
        remittanceService.saveRemittance(
                new RemittanceSaveRequest("SGD", BigDecimal.valueOf(1_000_000)));
        flushAndClear();

        LatestRemittanceResponse result = remittanceService.getLatestRemittance();

        assertThat(result.hasHistory()).isTrue();
        assertThat(result.receive().currency()).isEqualTo("SGD");
        assertThat(result.send().amount()).isEqualTo(new BigDecimal("1000000"));
    }

    @Test
    @DisplayName("지원하지 않는 통화면 저장하지 않고 예외를 던진다")
    void throwsWhenCurrencyUnknown() {
        assertThatThrownBy(() -> remittanceService.saveRemittance(
                new RemittanceSaveRequest("XXX", BigDecimal.valueOf(1_000_000))))
                .isInstanceOf(RemittanceException.class)
                .extracting(exception -> ((RemittanceException) exception).getErrorCode())
                .isEqualTo(RemittanceErrorCode.RECEIVE_CURRENCY_NOT_FOUND);

        assertThat(recentRemittanceRepository.count()).isZero();
    }

    @Test
    @DisplayName("비활성 통화도 없는 통화와 같은 예외로 막는다")
    void throwsWhenCurrencyInactive() {
        saveInactiveCurrency();
        flushAndClear();

        assertThatThrownBy(() -> remittanceService.saveRemittance(
                new RemittanceSaveRequest("THB", BigDecimal.valueOf(1_000_000))))
                .isInstanceOf(RemittanceException.class)
                .extracting(exception -> ((RemittanceException) exception).getErrorCode())
                .isEqualTo(RemittanceErrorCode.RECEIVE_CURRENCY_NOT_FOUND);

        assertThat(recentRemittanceRepository.count()).isZero();
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
