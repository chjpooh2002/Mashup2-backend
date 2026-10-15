package com.ceos24.mashup2backend.domain.receivemethod.dto.response;

import java.util.List;

public record ReceiveOptionDetailResponse(

        // 속도가 빠른 순으로 정렬된 요금제 목록. 선택 불가 옵션은 제외된다
        List<ReceiveOptionPlanResponse> plans

) {
}
