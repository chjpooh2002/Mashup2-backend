package com.ceos24.mashup2backend.domain.exchangerate.controller;

import com.ceos24.mashup2backend.domain.exchangerate.dto.request.ExchangeRateRequest;
import com.ceos24.mashup2backend.domain.exchangerate.dto.response.ExchangeRateResponse;
import com.ceos24.mashup2backend.domain.exchangerate.exception.code.ExchangeRateSuccessCode;
import com.ceos24.mashup2backend.domain.exchangerate.service.ExchangeRateQueryService;
import com.ceos24.mashup2backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exchange-rates")
@RequiredArgsConstructor
@Tag(name = "환율", description = "받는 통화의 현재 환율 조회")
public class ExchangeRateController {

    private final ExchangeRateQueryService exchangeRateQueryService;

    @Operation(
            summary = "환율 조회",
            description = """
                    받는 통화의 현재 환율을 조회한다. 서버에 저장된 최신 환율만 읽고 외부 환율 API는 호출하지 않는다.
                    보내는 통화는 KRW만 지원하고, 없는 통화와 비활성 통화는 같은 404로 응답한다.
                    환율 표시 방향은 통화에 따라 뒤집힌다 — 1원당 값이 1보다 작은 통화(ex. SGD)는
                    0.00095처럼 읽히지 않도록 from/to가 반대로 와서 "1 SGD = N KRW"를 뜻한다.
                    rate는 소수점 2자리로 반올림한 표시용 값이라 금액 환산에 쓰지 않는다. baseDate는 환율 고시 시각이다.
                    """)
    @GetMapping
    public ApiResponse<ExchangeRateResponse> getExchangeRate(
            @ParameterObject @Valid @ModelAttribute ExchangeRateRequest request) {

        return ApiResponse.onSuccess(
                ExchangeRateSuccessCode.EXCHANGE_RATE_OK,
                exchangeRateQueryService.getExchangeRate(request));
    }
}
