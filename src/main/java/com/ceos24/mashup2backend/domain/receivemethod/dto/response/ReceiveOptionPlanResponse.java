package com.ceos24.mashup2backend.domain.receivemethod.dto.response;

import com.ceos24.mashup2backend.domain.receivemethod.entity.enums.SpeedType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

// 받는 방법 하나의 속도별 요금제
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReceiveOptionPlanResponse(

        SpeedType speedType, // EXPRESS or STANDARD

        // 송금 시간 문구. SpeedType.transferTime
        String transferTime,

        // 고정 수수료. 보내는 통화(KRW) 기준
        BigDecimal fixedFee

) {
}
