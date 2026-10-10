package com.ceos24.mashup2backend.domain.receivemethod.converter;

import com.ceos24.mashup2backend.domain.receivemethod.dto.enums.BadgeType;
import com.ceos24.mashup2backend.domain.receivemethod.dto.response.ReceiveOptionListResponse;
import com.ceos24.mashup2backend.domain.receivemethod.dto.response.ReceiveOptionResponse;
import com.ceos24.mashup2backend.domain.receivemethod.entity.ReceiveMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReceiveOptionConverter {

    // 선택 가능한 옵션. 수령액과 도착일은 서비스에서 계산해서 넘겨받는다
    public static ReceiveOptionResponse     toSelectableResponse(ReceiveMethod method,
                                                             BigDecimal totalAmount,
                                                             LocalDate arrivalDate,
                                                             boolean recommended) {
        return new ReceiveOptionResponse(
                method.getMethodType(),
                method.getMethodProvider(),
                method.getMethodName(),
                method.getSpeedType(),
                method.getFixedFee(),
                method.getRateMargin(),
                totalAmount,
                method.getSpeedType().getTransferTime(),
                arrivalDate,
                recommended ? List.of(BadgeType.RECOMMENDED) : null,
                null);
    }

    // 선택 불가 옵션은 수수료/수령액/도착일/뱃지를 비워 응답에서 제외시킨다
    public static ReceiveOptionResponse toUnselectableResponse(ReceiveMethod method) {
        return new ReceiveOptionResponse(
                method.getMethodType(),
                method.getMethodProvider(),
                method.getMethodName(),
                method.getSpeedType(),
                null,
                null,
                null,
                method.getSpeedType().getTransferTime(),
                null,
                null,
                method.getUnselectableReason());
    }

    // toSelectableResponse(선택 가능 리스트) + toUnselectableResponse(선택 불가 리스트)
    public static ReceiveOptionListResponse toListResponse(String sendCurrency,
                                                           String receiveCurrency,
                                                           BigDecimal sendAmount,
                                                           List<ReceiveOptionResponse> options) {
        return new ReceiveOptionListResponse(sendCurrency, receiveCurrency, sendAmount, options);
    }
}
