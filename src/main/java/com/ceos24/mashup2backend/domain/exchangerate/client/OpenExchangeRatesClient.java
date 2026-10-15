package com.ceos24.mashup2backend.domain.exchangerate.client;

import com.ceos24.mashup2backend.domain.exchangerate.client.dto.OpenExchangeRatesResponse;
import com.ceos24.mashup2backend.domain.exchangerate.exception.ExchangeRateException;
import com.ceos24.mashup2backend.domain.exchangerate.exception.code.ExchangeRateErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Collection;

@Slf4j
@Component
public class OpenExchangeRatesClient {

    private static final String BASE_URL = "https://openexchangerates.org/api";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final String appId;

    public OpenExchangeRatesClient(@Value("${open-exchange-rates.app-id:}") String appId) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .build();
        this.appId = appId;
    }

    // 앱 키가 설정되어 있는지 여부. 비어 있으면 호출해도 401이므로 호출 전에 확인한다
    public boolean hasAppId() {
        return StringUtils.hasText(appId);
    }

    /***
     * 함수 기능: USD 기준 최신 환율을 조회한다.
     * @param symbols 조회할 통화 코드 목록 (KRW 포함)
     * @return OpenExchangeRatesResponse (고시 시각, 기준 통화, 통화별 환율)
     */
    public OpenExchangeRatesResponse fetchLatest(Collection<String> symbols) {
        OpenExchangeRatesResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/latest.json")
                            .queryParam("app_id", appId)
                            .queryParam("symbols", String.join(",", symbols))
                            .build())
                    .retrieve()
                    .body(OpenExchangeRatesResponse.class);
        } catch (RestClientResponseException e) {
            // 예외 메시지에는 app_id가 담긴 URL이 들어가므로 상태 코드만 남긴다
            log.error("환율 API 응답 오류 - 상태 코드: {}", e.getStatusCode().value());
            throw new ExchangeRateException(ExchangeRateErrorCode.RATES_API_CALL_FAILED);
        } catch (RestClientException e) {
            // 마찬가지로 메시지 대신 예외 종류만 남긴다
            log.error("환율 API 호출 실패 - 예외: {}", e.getClass().getSimpleName());
            throw new ExchangeRateException(ExchangeRateErrorCode.RATES_API_CALL_FAILED);
        }

        if (response == null) {
            log.error("환율 API 응답 본문이 비어 있습니다.");
            throw new ExchangeRateException(ExchangeRateErrorCode.RATES_API_CALL_FAILED);
        }
        return response;
    }
}
