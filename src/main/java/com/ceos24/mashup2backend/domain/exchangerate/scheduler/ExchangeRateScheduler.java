package com.ceos24.mashup2backend.domain.exchangerate.scheduler;

import com.ceos24.mashup2backend.domain.exchangerate.service.ExchangeRateSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 운영에서만 등록한다. 무료 플랜 호출 한도(월 1,000회) 때문에 로컬·테스트에서는 빈 자체를 만들지 않는다
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    private final ExchangeRateSyncService exchangeRateSyncService;

    // 매시 5분. 시간대와 무관한 일정이라 zone을 지정하지 않는다
    @Scheduled(cron = "0 5 * * * *")
    public void syncExchangeRates() {
        log.info("환율 갱신 스케줄 시작");
        exchangeRateSyncService.sync();
        log.info("환율 갱신 스케줄 종료");
    }
}
