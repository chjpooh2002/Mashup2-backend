package com.ceos24.mashup2backend.domain.exchangerate.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ExchangeRateRequest(

        // 보내는 통화. (현재는 KRW만 존재)
        @NotBlank(message = "보내는 통화는 필수입니다.")
        String sendCurrency,

        // 받는 통화 (ex. VND)
        @NotBlank(message = "받는 통화는 필수입니다.")
        String receiveCurrency

) {
}
