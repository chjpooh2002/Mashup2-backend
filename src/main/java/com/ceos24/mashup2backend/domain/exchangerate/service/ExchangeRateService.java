package com.ceos24.mashup2backend.domain.exchangerate.service;

import com.ceos24.mashup2backend.domain.currency.repository.CurrencyRepository;
import com.ceos24.mashup2backend.domain.exchangerate.entity.ExchangeRate;
import com.ceos24.mashup2backend.domain.exchangerate.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyRepository currencyRepository;

    /***
     * 함수 기능: 환산된 환율을 하나의 트랜잭션으로 저장한다. 행이 있으면 갱신, 없으면 생성한다.
     * @param krwRates 통화별 1원당 환율과 고시 시각
     * @param updatedAt 저장 시각
     */
    @Transactional
    public void saveRates(KrwRates krwRates, LocalDateTime updatedAt) {
        Map<String, ExchangeRate> existing = exchangeRateRepository.findAllById(krwRates.rates().keySet())
                .stream()
                .collect(Collectors.toMap(ExchangeRate::getCurrencyCode, Function.identity()));

        krwRates.rates().forEach((code, rate) -> {
            ExchangeRate exchangeRate = existing.get(code);
            if (exchangeRate != null) {
                // 변경 감지로 반영된다
                exchangeRate.refresh(rate, krwRates.baseTime(), updatedAt);
                return;
            }
            exchangeRateRepository.save(newExchangeRate(code, rate, krwRates.baseTime(), updatedAt));
        });
    }

    private ExchangeRate newExchangeRate(String code, BigDecimal rate,
                                         LocalDateTime baseTime, LocalDateTime updatedAt) {
        return ExchangeRate.builder()
                .currency(currencyRepository.getReferenceById(code))
                .baseRate(rate)
                .baseTime(baseTime)
                .updatedAt(updatedAt)
                .build();
    }
}
