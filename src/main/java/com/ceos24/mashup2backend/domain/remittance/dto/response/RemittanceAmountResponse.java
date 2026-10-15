package com.ceos24.mashup2backend.domain.remittance.dto.response;

import java.math.BigDecimal;

// 보내는 쪽·받는 쪽 공용 금액 블록
public record RemittanceAmountResponse(

        // 국가 코드 (ex. KR, VN)
        String countryCode,

        // 통화 코드 (ex. KRW, VND)
        String currency,

        // 금액. 보내는 쪽은 원화, 받는 쪽은 환산된 받는 통화 금액
        BigDecimal amount

) {
}
