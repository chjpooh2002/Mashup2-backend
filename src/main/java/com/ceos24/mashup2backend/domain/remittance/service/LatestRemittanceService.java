package com.ceos24.mashup2backend.domain.remittance.service;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import com.ceos24.mashup2backend.domain.currency.repository.CurrencyRepository;
import com.ceos24.mashup2backend.domain.exchangerate.entity.ExchangeRate;
import com.ceos24.mashup2backend.domain.exchangerate.repository.ExchangeRateRepository;
import com.ceos24.mashup2backend.domain.remittance.converter.LatestRemittanceConverter;
import com.ceos24.mashup2backend.domain.remittance.dto.response.LatestRemittanceResponse;
import com.ceos24.mashup2backend.domain.remittance.entity.RecentRemittance;
import com.ceos24.mashup2backend.domain.remittance.exception.RemittanceException;
import com.ceos24.mashup2backend.domain.remittance.exception.code.RemittanceErrorCode;
import com.ceos24.mashup2backend.domain.remittance.repository.RecentRemittanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LatestRemittanceService {

    // 인증이 없어 유저 ID는 1로 고정한다
    private static final Long FIXED_USER_ID = 1L;

    // 보내는 쪽은 KRW만 지원한다. currency 테이블은 받는 통화만 담으므로 상수로 둔다
    private static final String SEND_COUNTRY_CODE = "KR";
    private static final String SEND_CURRENCY_CODE = "KRW";
    private static final int SEND_DECIMAL_PLACES = 0;

    // 쓸 수 있는 이력이 없을 때 쓰는 기본값
    private static final String DEFAULT_RECEIVE_CURRENCY_CODE = "VND";
    private static final BigDecimal DEFAULT_SEND_AMOUNT = BigDecimal.valueOf(10_000);

    private final RecentRemittanceRepository recentRemittanceRepository;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    /***
     * 함수 기능: 유저의 최근 송금 이력과 그 나라의 현재 환율을 조회한다.
     * 이력이 없거나 이력의 받는 통화를 쓸 수 없으면 KRW 10,000 -> VND 기본값으로 응답한다.
     * @return LatestRemittanceResponse (이력 보유 여부 + 보내는/받는 금액 + 현재 환율)
     */
    public LatestRemittanceResponse getLatestRemittance() {

        // flatMap
        // i) findFirstByUserIdOrderBySentAtDescIdDesc가 비어있다면 -> toQuote을 호출하지 않고 Optional.empty() 반환
        // ii) 값이 있다면 -> toQuote(remittance)의 결과(Optional<Quote>)를 그대로 반환

        // map
        // i) 값이 없다면 -> 람다를 호출하지 않고 Optional.empty() 반환
        // ii) 값이 있다면 -> toResponse(true, quote) 결과를 Optional로 감싸서 반환
        return recentRemittanceRepository.findFirstByUserIdOrderBySentAtDescIdDesc(FIXED_USER_ID)
                .flatMap(this::toQuote)
                .map(quote -> toResponse(true, quote))
                .orElseGet(() -> toResponse(false, defaultQuote())); // Optional.empty()라면 호출
    }

    // 최근 송금 이력이 존재하지만, 이력의 받는 통화가 비활성이거나 환율이 없으면 비어 있다 (기본값 폴백 대상)
    private Optional<Quote> toQuote(RecentRemittance remittance) {

        Currency receiveCurrency = remittance.getCurrency();
        String receiveCurrencyCode = receiveCurrency.getCurrencyCode();

        // 받는 통화가 비활성이라면, Optional.emtpy() 반환
        if (!receiveCurrency.getIsActive()) {
            log.warn("최근 송금 이력의 받는 통화({})가 비활성이라 기본값으로 응답한다", receiveCurrencyCode);
            return Optional.empty();
        }

        // 받는 통화의 환율 정보가 없다면, Optional.empty() 반환
        Optional<ExchangeRate> exchangeRate = exchangeRateRepository.findById(receiveCurrencyCode);
        if (exchangeRate.isEmpty()) {
            log.warn("최근 송금 이력의 받는 통화({}) 환율이 없어 기본값으로 응답한다", receiveCurrencyCode);
            return Optional.empty();
        }

        return Optional.of(new Quote(receiveCurrency, exchangeRate.get(), remittance.getSendAmount()));
    }

    // 최근 송금 이력이 없다면, 받는 통화는 VND와 송금 금액은 10,000KRW 기준으로 한다.
    private Quote defaultQuote() {

        Currency receiveCurrency = currencyRepository
                .findByCurrencyCodeAndIsActiveTrue(DEFAULT_RECEIVE_CURRENCY_CODE)
                .orElseThrow(() -> new RemittanceException(RemittanceErrorCode.DEFAULT_CURRENCY_NOT_FOUND));

        ExchangeRate exchangeRate = exchangeRateRepository.findById(DEFAULT_RECEIVE_CURRENCY_CODE)
                .orElseThrow(() -> new RemittanceException(RemittanceErrorCode.DEFAULT_EXCHANGE_RATE_NOT_FOUND));

        return new Quote(receiveCurrency, exchangeRate, DEFAULT_SEND_AMOUNT);
    }

    // LatestRemittanceResponse을 생성한다.
    private LatestRemittanceResponse toResponse(boolean hasHistory, Quote quote) {

        // 받는 금액은 표시용 환율이 아니라 baseRate로 계산한다. 반올림한 표시 환율로 역산하면 금액이 틀어진다
        BigDecimal receiveAmount = quote.exchangeRate()
                .convertFromKrw(quote.sendAmount(), quote.receiveCurrency().getDecimalPlaces());

        // sendAmount는 컬럼 scale이 4라 그대로 내보내면 1000000.0000이 된다.
        // stripTrailingZeros()는 1E+6으로 만들어버리므로 원화 자릿수(0)로 맞춘다
        BigDecimal sendAmount = quote.sendAmount().setScale(SEND_DECIMAL_PLACES, RoundingMode.HALF_UP);

        return LatestRemittanceConverter.toLatestRemittanceResponse(
                hasHistory,
                LatestRemittanceConverter.toSendResponse(SEND_COUNTRY_CODE, SEND_CURRENCY_CODE, sendAmount),
                LatestRemittanceConverter.toReceiveResponse(quote.receiveCurrency(), receiveAmount),
                LatestRemittanceConverter.toExchangeRateResponse(
                        quote.exchangeRate(), quote.receiveCurrency(), SEND_CURRENCY_CODE));
    }

    // 응답을 만드는 데 필요한 값 묶음. 이력이 없으면 기본값으로 채운다
    private record Quote(Currency receiveCurrency, ExchangeRate exchangeRate, BigDecimal sendAmount) {
    }
}
