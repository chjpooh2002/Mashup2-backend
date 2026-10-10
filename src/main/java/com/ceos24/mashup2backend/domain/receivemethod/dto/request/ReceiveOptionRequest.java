package com.ceos24.mashup2backend.domain.receivemethod.dto.request;

import com.ceos24.mashup2backend.domain.receivemethod.dto.enums.SortType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ReceiveOptionRequest(

        // 보내는 통화. (현재는 KRW만 존재)
        @NotBlank(message = "보내는 통화는 필수입니다.")
        String sendCurrency,

        // 받는 통화 (ex. VND)
        @NotBlank(message = "받는 통화는 필수입니다.")
        String receiveCurrency,

        // 보내는 금액
        @NotNull(message = "보내는 금액은 필수입니다.")
        @Positive(message = "보내는 금액은 0보다 커야 합니다.")
        BigDecimal amount,

        // 정렬 기준. 미지정 시 RATE
        SortType sort

) {

    public ReceiveOptionRequest {
        if (sort == null) {
            sort = SortType.RATE;
        }
    }
}
