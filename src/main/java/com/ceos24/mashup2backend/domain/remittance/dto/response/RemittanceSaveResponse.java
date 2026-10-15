package com.ceos24.mashup2backend.domain.remittance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 저장된 값만 에코한다. 환율과 수령액은 컬럼이 없어 저장되지 않으므로 내리지 않는다
public record RemittanceSaveResponse(

        // 추가된 행의 id
        Long remittanceId,

        // 받는 통화 (ex. VND)
        String receiveCurrency,

        // 보낸 원화 금액
        BigDecimal sendAmount,

        // 서버가 채운 보낸 시각
        LocalDateTime sentAt

) {
}
