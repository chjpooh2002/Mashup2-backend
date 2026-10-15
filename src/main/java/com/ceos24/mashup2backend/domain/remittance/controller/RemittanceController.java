package com.ceos24.mashup2backend.domain.remittance.controller;

import com.ceos24.mashup2backend.domain.remittance.dto.response.LatestRemittanceResponse;
import com.ceos24.mashup2backend.domain.remittance.exception.code.RemittanceSuccessCode;
import com.ceos24.mashup2backend.domain.remittance.service.LatestRemittanceService;
import com.ceos24.mashup2backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/remittances")
@RequiredArgsConstructor
@Tag(name = "송금 이력", description = "최근 송금 이력 조회")
public class LatestRemittanceController {

    private final LatestRemittanceService latestRemittanceService;

    @Operation(
            summary = "최근 송금 이력 조회",
            description = """
                    최근에 송금했던 나라와 그 나라의 현재 환율을 조회한다. 홈 화면 초기값으로 쓴다.
                    인증이 없어 유저는 1로 고정이고 요청 파라미터는 없다.
                    이력이 없거나 이력의 받는 통화를 쓸 수 없으면 hasHistory=false와 함께 KRW 10,000 -> VND 기본값을 내려준다.
                    환율 표시 방향은 통화에 따라 뒤집힌다 — 1원당 값이 1보다 작은 통화(ex. SGD)는 from/to가 반대로 온다.
                    """)
    @GetMapping("/latest")
    public ApiResponse<LatestRemittanceResponse> getLatestRemittance() {

        return ApiResponse.onSuccess(
                RemittanceSuccessCode.LATEST_REMITTANCE_OK,
                latestRemittanceService.getLatestRemittance());
    }
}
