package com.ceos24.mashup2backend.domain.receivemethod.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ReceiveOptionListResponse(

        // 보내는 통화 (KRW)
        String sendCurrency,

        // 받는 통화 (ex. VND)
        String receiveCurrency,

        // 보내는 금액. 요청의 amount
        BigDecimal sendAmount,

        // 정렬된 수취 옵션 목록. 선택 불가 옵션은 항상 맨 뒤
        List<ReceiveOptionResponse> options

) {
}
