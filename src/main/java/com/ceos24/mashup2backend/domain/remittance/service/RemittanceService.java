package com.ceos24.mashup2backend.domain.remittance.service;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import com.ceos24.mashup2backend.domain.currency.repository.CurrencyRepository;
import com.ceos24.mashup2backend.domain.exchangerate.entity.ExchangeRate;
import com.ceos24.mashup2backend.domain.exchangerate.repository.ExchangeRateRepository;
import com.ceos24.mashup2backend.domain.remittance.converter.RemittanceConverter;
import com.ceos24.mashup2backend.domain.remittance.dto.request.RemittanceSaveRequest;
import com.ceos24.mashup2backend.domain.remittance.dto.response.LatestRemittanceResponse;
import com.ceos24.mashup2backend.domain.remittance.dto.response.RemittanceSaveResponse;
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
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RemittanceService {

    // 인증이 없어 유저 ID는 1로 고정한다
    private static final Long FIXED_USER_ID = 1L;

    // 보내는 쪽은 KRW만 지원한다. currency 테이블은 받는 통화만 담으므로 상수로 둔다
    private static final String SEND_COUNTRY_CODE = "KR";
    private static final String SEND_CURRENCY_CODE = "KRW";

    // 원화 금액 자릿수. 컬럼 scale이 4라 그대로 내보내면 1000000.0000이 된다
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

        return recentRemittanceRepository.findFirstByUserIdOrderBySentAtDescIdDesc(FIXED_USER_ID)
                .flatMap(this::toQuote)
                .map(quote -> toResponse(true, quote))
                .orElseGet(() -> toResponse(false, defaultQuote()));
    }

    /***
     * 함수 기능: 송금 폼 제출 내용을 최근 송금 이력으로 저장한다.
     * @param request RemittanceSaveRequest (받는 통화, 보낸 원화 금액)
     * @return RemittanceSaveResponse (추가된 행의 id + 저장된 값)
     */
    @Transactional
    public RemittanceSaveResponse saveRemittance(RemittanceSaveRequest request) {

        // 없는 통화와 비활성 통화를 같은 에러로 처리한다
        Currency receiveCurrency = currencyRepository
                .findByCurrencyCodeAndIsActiveTrue(request.receiveCurrency())
                .orElseThrow(() -> new RemittanceException(RemittanceErrorCode.RECEIVE_CURRENCY_NOT_FOUND));

        // 폼 제출마다 행을 추가한다. 같은 통화의 기존 행을 덮어쓰지 않으므로 같은 요청을 두 번 보내면 두 건이 쌓인다
        RecentRemittance saved = recentRemittanceRepository.save(RecentRemittance.builder()
                .currency(receiveCurrency)
                .userId(FIXED_USER_ID) // userId는 1로 고정
                .sendAmount(request.sendAmount())
                .sentAt(LocalDateTime.now())
                .build());

        return RemittanceConverter.toSaveResponse(saved, toSendAmount(request.sendAmount()));
    }

    // 이력의 받는 통화가 비활성이거나 환율이 없으면 비어 있다 (기본값 폴백 대상)
    private Optional<Quote> toQuote(RecentRemittance remittance) {

        Currency receiveCurrency = remittance.getCurrency();
        String receiveCurrencyCode = receiveCurrency.getCurrencyCode();

        if (!receiveCurrency.getIsActive()) {
            log.warn("최근 송금 이력의 받는 통화({})가 비활성이라 기본값으로 응답한다", receiveCurrencyCode);
            return Optional.empty();
        }

        Optional<ExchangeRate> exchangeRate = exchangeRateRepository.findById(receiveCurrencyCode);
        if (exchangeRate.isEmpty()) {
            log.warn("최근 송금 이력의 받는 통화({}) 환율이 없어 기본값으로 응답한다", receiveCurrencyCode);
            return Optional.empty();
        }

        return Optional.of(new Quote(receiveCurrency, exchangeRate.get(), remittance.getSendAmount()));
    }

    // 기본값(VND)마저 없으면 더 이상 폴백할 곳이 없어 서버 데이터 정합성 문제로 본다
    private Quote defaultQuote() {

        Currency receiveCurrency = currencyRepository
                .findByCurrencyCodeAndIsActiveTrue(DEFAULT_RECEIVE_CURRENCY_CODE)
                .orElseThrow(() -> new RemittanceException(RemittanceErrorCode.DEFAULT_CURRENCY_NOT_FOUND));

        ExchangeRate exchangeRate = exchangeRateRepository.findById(DEFAULT_RECEIVE_CURRENCY_CODE)
                .orElseThrow(() -> new RemittanceException(RemittanceErrorCode.DEFAULT_EXCHANGE_RATE_NOT_FOUND));

        return new Quote(receiveCurrency, exchangeRate, DEFAULT_SEND_AMOUNT);
    }

    private LatestRemittanceResponse toResponse(boolean hasHistory, Quote quote) {

        // 받는 금액은 표시용 환율이 아니라 baseRate로 계산한다. 반올림한 표시 환율로 역산하면 금액이 틀어진다
        BigDecimal receiveAmount = quote.exchangeRate()
                .convertFromKrw(quote.sendAmount(), quote.receiveCurrency().getDecimalPlaces());

        return RemittanceConverter.toLatestRemittanceResponse(
                hasHistory,
                RemittanceConverter.toSendResponse(
                        SEND_COUNTRY_CODE, SEND_CURRENCY_CODE, toSendAmount(quote.sendAmount())),
                RemittanceConverter.toReceiveResponse(quote.receiveCurrency(), receiveAmount),
                RemittanceConverter.toExchangeRateResponse(
                        quote.exchangeRate(), quote.receiveCurrency(), SEND_CURRENCY_CODE));
    }

    // 원화 금액 자릿수로 맞춘다. stripTrailingZeros()는 1000000을 1E+6으로 만들어버린다
    private BigDecimal toSendAmount(BigDecimal sendAmount) {
        return sendAmount.setScale(SEND_DECIMAL_PLACES, RoundingMode.HALF_UP);
    }

    // 응답을 만드는 데 필요한 값 묶음. 이력이 없으면 기본값으로 채운다
    private record Quote(Currency receiveCurrency, ExchangeRate exchangeRate, BigDecimal sendAmount) {
    }
}
