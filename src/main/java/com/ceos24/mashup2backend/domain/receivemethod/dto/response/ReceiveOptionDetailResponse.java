package com.ceos24.mashup2backend.domain.receivemethod.dto.response;

import java.util.List;

// 받는 방법 상세 조회 응답 result 본문
public record ReceiveOptionDetailResponse(

        // 속도가 빠른 순으로 정렬된 요금제 목록. 선택 불가 옵션은 제외된다
        List<ReceiveOptionPlanResponse> plans

) {
}
