package com.ceos24.mashup2backend.domain.exchangerate.service;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import com.ceos24.mashup2backend.domain.currency.repository.CurrencyRepository;
import com.ceos24.mashup2backend.domain.exchangerate.client.OpenExchangeRatesClient;
import com.ceos24.mashup2backend.domain.exchangerate.client.dto.OpenExchangeRatesResponse;
import com.ceos24.mashup2backend.domain.exchangerate.exception.ExchangeRateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 외부 호출 동안 DB 커넥션을 잡지 않도록 트랜잭션을 걸지 않는다. 저장만 ExchangeRateService에서 트랜잭션으로 처리한다
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateSyncService {

    private final OpenExchangeRatesClient openExchangeRatesClient;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateService exchangeRateService;

    /***
     * 함수 기능: 활성 통화의 환율을 외부 API에서 받아 갱신한다.
     * 실패하거나 응답이 이상하면 기존 값을 유지하고 로그만 남긴다. 호출 한도 때문에 재시도하지 않는다.
     */
    public void sync() {
        if (!openExchangeRatesClient.hasAppId()) {
            log.warn("Open Exchange Rates 앱 키 미설정 - 환율 갱신을 건너뜁니다.");
            return;
        }

        List<String> targetCodes = currencyRepository.findAllByIsActiveTrue().stream()
                .map(Currency::getCurrencyCode)
                .toList();
        if (targetCodes.isEmpty()) {
            log.warn("활성 통화가 없어 환율 갱신을 건너뜁니다.");
            return;
        }

        // 원화는 currency 테이블에 없으므로 호출할 때만 붙인다
        List<String> symbols = new ArrayList<>(targetCodes);
        symbols.add(KrwRateCalculator.KRW);

        try {
            OpenExchangeRatesResponse response = openExchangeRatesClient.fetchLatest(symbols);
            KrwRates krwRates = KrwRateCalculator.calculate(response, targetCodes);
            exchangeRateService.saveRates(krwRates, LocalDateTime.now());
            log.info("환율 갱신 완료 - 대상: {}, 고시 시각: {}", targetCodes, krwRates.baseTime());
        } catch (ExchangeRateException e) {
            log.error("환율 갱신 실패, 기존 값을 유지합니다 - {} ({})",
                    e.getErrorCode().getMessage(), e.getErrorCode().getCode());
        }
    }
}
