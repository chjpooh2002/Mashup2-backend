package com.ceos24.mashup2backend.domain.exchangerate.converter;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import com.ceos24.mashup2backend.domain.exchangerate.dto.response.ExchangeRateResponse;
import com.ceos24.mashup2backend.domain.exchangerate.entity.ExchangeRate;

public class ExchangeRateConverter {

    private ExchangeRateConverter() {
    }

    // 표시 방향과 표시용 환율은 ExchangeRate가 판단한다. 컨버터는 from/to 자리만 바꾼다
    public static ExchangeRateResponse toExchangeRateResponse(ExchangeRate exchangeRate,
                                                              Currency receiveCurrency,
                                                              String sendCurrencyCode) {
        boolean krwBased = exchangeRate.isKrwBasedDisplay();
        String receiveCurrencyCode = receiveCurrency.getCurrencyCode();

        return new ExchangeRateResponse(
                krwBased ? sendCurrencyCode : receiveCurrencyCode,
                krwBased ? receiveCurrencyCode : sendCurrencyCode,
                exchangeRate.displayRate(),
                exchangeRate.getBaseTime());
    }
}
