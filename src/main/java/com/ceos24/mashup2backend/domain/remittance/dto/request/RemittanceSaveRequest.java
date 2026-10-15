package com.ceos24.mashup2backend.domain.remittance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RemittanceSaveRequest(

        // 받는 통화 (ex. VND). 컬럼명은 currency_code지만 의미는 받는 통화다
        @NotBlank(message = "받는 통화는 필수입니다.")
        String receiveCurrency,

        // 보낸 원화 금액. 수단별 한도는 검증하지 않는다
        @NotNull(message = "보내는 금액은 필수입니다.")
        @Positive(message = "보내는 금액은 0보다 커야 합니다.")
        BigDecimal sendAmount

) {
}
