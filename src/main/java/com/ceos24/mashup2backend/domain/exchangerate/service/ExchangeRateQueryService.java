package com.ceos24.mashup2backend.domain.exchangerate.service;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import com.ceos24.mashup2backend.domain.currency.repository.CurrencyRepository;
import com.ceos24.mashup2backend.domain.exchangerate.converter.ExchangeRateConverter;
import com.ceos24.mashup2backend.domain.exchangerate.dto.request.ExchangeRateRequest;
import com.ceos24.mashup2backend.domain.exchangerate.dto.response.ExchangeRateResponse;
import com.ceos24.mashup2backend.domain.exchangerate.entity.ExchangeRate;
import com.ceos24.mashup2backend.domain.exchangerate.exception.ExchangeRateException;
import com.ceos24.mashup2backend.domain.exchangerate.exception.code.ExchangeRateErrorCode;
import com.ceos24.mashup2backend.domain.exchangerate.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 환율 조회 전용. 저장은 ExchangeRateService, 외부 API 호출은 ExchangeRateSyncService가 맡는다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeRateQueryService {

    // 보내는 통화는 KRW만 지원한다
    private static final String SUPPORTED_SEND_CURRENCY = "KRW";

    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    /***
     * 함수 기능: 받는 통화의 저장된 현재 환율을 화면 표시 방향으로 조회한다. 외부 API는 호출하지 않는다.
     * @param request ExchangeRateRequest (보내는 통화, 받는 통화)
     * @return ExchangeRateResponse (표시 방향 from/to + 표시용 환율 + 고시 시각)
     */
    public ExchangeRateResponse getExchangeRate(ExchangeRateRequest request) {

        // 현 정책 상 오직 KRW 통화만 받는다.
        if (!SUPPORTED_SEND_CURRENCY.equals(request.sendCurrency())) {
            throw new ExchangeRateException(ExchangeRateErrorCode.UNSUPPORTED_SEND_CURRENCY);
        }

        // 없는 통화와 비활성 통화를 같은 에러로 처리한다
        Currency receiveCurrency = currencyRepository.findByCurrencyCodeAndIsActiveTrue(request.receiveCurrency())
                .orElseThrow(() -> new ExchangeRateException(ExchangeRateErrorCode.RECEIVE_CURRENCY_NOT_FOUND));

        // 활성 통화인데 환율이 없으면 서버 데이터 정합성 문제다
        ExchangeRate exchangeRate = exchangeRateRepository.findById(receiveCurrency.getCurrencyCode())
                .orElseThrow(() -> new ExchangeRateException(ExchangeRateErrorCode.EXCHANGE_RATE_NOT_FOUND));

        return ExchangeRateConverter.toExchangeRateResponse(exchangeRate, receiveCurrency, SUPPORTED_SEND_CURRENCY);
    }
}
