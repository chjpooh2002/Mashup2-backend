package com.ceos24.mashup2backend.domain.receivemethod.dto.response;

import com.ceos24.mashup2backend.domain.receivemethod.dto.enums.BadgeType;
import com.ceos24.mashup2backend.domain.receivemethod.entity.enums.MethodProvider;
import com.ceos24.mashup2backend.domain.receivemethod.entity.enums.MethodType;
import com.ceos24.mashup2backend.domain.receivemethod.entity.enums.SpeedType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReceiveOptionResponse(

        MethodType methodType, // 받는 방법
        MethodProvider methodProvider, // 받는 방법 제공자
        String methodName, // 받는 방법 이름
        SpeedType speedType, // EXPRESS or STANDARD

        // 고정 수수료. 보내는 통화(KRW) 기준
        BigDecimal fixedFee,

        // 비율 마진 (0.001 = 0.1%)
        BigDecimal rateMargin,

        // 수령액. sendAmount * baseRate * (1 - rateMargin)을 decimalPlaces 자리로 올림
        BigDecimal totalAmount,

        // 송금 시간 문구. SpeedType.transferTime
        String transferTime,

        // 도착 예정일. 조회일 + SpeedType.arrivalDays
        LocalDate arrivalDate,

        List<BadgeType> badges,

        // 선택 불가 사유. 선택 가능하면 null
        String unselectableReason

) {
}
