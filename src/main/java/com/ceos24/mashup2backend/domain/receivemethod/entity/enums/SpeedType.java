package com.ceos24.mashup2backend.domain.receivemethod.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SpeedType {

    EXPRESS("15분 ~ 1일", 0),
    STANDARD("영업일 2~3일", 2);

    private final String transferTime;
    private final int arrivalDays;
}