package com.ceos24.mashup2backend.domain.receivemethod.dto.request;

import com.ceos24.mashup2backend.domain.receivemethod.entity.enums.MethodProvider;
import com.ceos24.mashup2backend.domain.receivemethod.entity.enums.MethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReceiveOptionDetailRequest(

        // 받는 통화 (ex. VND)
        @NotBlank(message = "받는 통화는 필수입니다.")
        String receiveCurrency,

        // 받는 방법 (ex. BANK, CARD)
        @NotNull(message = "받는 방법은 필수입니다.")
        MethodType methodType,

        // 제공사 (ex. ATM, GOPAY). 제공사가 없는 수단은 NONE을 보낸다
        @NotNull(message = "제공사는 필수입니다.")
        MethodProvider methodProvider

) {
}
