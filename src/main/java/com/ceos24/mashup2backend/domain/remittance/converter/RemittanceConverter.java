package com.ceos24.mashup2backend.domain.remittance.converter;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import com.ceos24.mashup2backend.domain.exchangerate.entity.ExchangeRate;
import com.ceos24.mashup2backend.domain.remittance.dto.response.LatestRemittanceResponse;
import com.ceos24.mashup2backend.domain.remittance.dto.response.RemittanceAmountResponse;
import com.ceos24.mashup2backend.domain.remittance.dto.response.RemittanceExchangeRateResponse;
import com.ceos24.mashup2backend.domain.remittance.dto.response.RemittanceSaveResponse;
import com.ceos24.mashup2backend.domain.remittance.entity.RecentRemittance;

import java.math.BigDecimal;

public class RemittanceConverter {

    private RemittanceConverter() {
    }

    // 보내는 쪽. KRW는 currency 테이블에 없어 코드를 그대로 받는다
    public static RemittanceAmountResponse toSendResponse(String countryCode, String currencyCode,
                                                          BigDecimal amount) {
        return new RemittanceAmountResponse(countryCode, currencyCode, amount);
    }

    // 받는 쪽. 환산 금액은 서비스에서 계산해서 넘겨받는다
    public static RemittanceAmountResponse toReceiveResponse(Currency receiveCurrency, BigDecimal amount) {
        return new RemittanceAmountResponse(
                receiveCurrency.getCountryCode(),
                receiveCurrency.getCurrencyCode(),
                amount);
    }

    // 표시 방향과 표시용 환율은 ExchangeRate가 판단한다. 컨버터는 from/to 자리만 바꾼다
    public static RemittanceExchangeRateResponse toExchangeRateResponse(ExchangeRate exchangeRate,
                                                                        Currency receiveCurrency,
                                                                        String sendCurrencyCode) {
        boolean krwBased = exchangeRate.isKrwBasedDisplay();
        String receiveCurrencyCode = receiveCurrency.getCurrencyCode();

        return new RemittanceExchangeRateResponse(
                krwBased ? sendCurrencyCode : receiveCurrencyCode,
                krwBased ? receiveCurrencyCode : sendCurrencyCode,
                exchangeRate.displayRate(),
                exchangeRate.getBaseTime());
    }

    // 저장 결과. 금액은 원화 자릿수로 맞춰 서비스에서 넘겨받는다
    public static RemittanceSaveResponse toSaveResponse(RecentRemittance remittance, BigDecimal sendAmount) {
        return new RemittanceSaveResponse(
                remittance.getId(),
                remittance.getCurrency().getCurrencyCode(),
                sendAmount,
                remittance.getSentAt());
    }

    public static LatestRemittanceResponse toLatestRemittanceResponse(
            boolean hasHistory,
            RemittanceAmountResponse send,
            RemittanceAmountResponse receive,
            RemittanceExchangeRateResponse exchangeRate) {
        return new LatestRemittanceResponse(hasHistory, send, receive, exchangeRate);
    }
}
