package com.ceos24.mashup2backend.domain.remittance.controller;

import com.ceos24.mashup2backend.domain.remittance.dto.request.RemittanceSaveRequest;
import com.ceos24.mashup2backend.domain.remittance.dto.response.LatestRemittanceResponse;
import com.ceos24.mashup2backend.domain.remittance.dto.response.RemittanceSaveResponse;
import com.ceos24.mashup2backend.domain.remittance.exception.code.RemittanceSuccessCode;
import com.ceos24.mashup2backend.domain.remittance.service.RemittanceService;
import com.ceos24.mashup2backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/remittances")
@RequiredArgsConstructor
@Tag(name = "송금 이력", description = "최근 송금 이력 조회 및 저장")
public class RemittanceController {

    private final RemittanceService remittanceService;

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
                remittanceService.getLatestRemittance());
    }

    @Operation(
            summary = "송금 이력 저장",
            description = """
                    송금 폼 제출 내용을 최근 송금 이력으로 저장한다. 다음 접속 때 조회 API가 이 행을 읽는다.
                    최근에 보낸 나라와 금액을 기억하기 위한 것이라 수취 수단과 적용 환율은 저장하지 않는다.
                    폼 제출마다 행이 추가되므로 같은 요청을 두 번 보내면 두 건이 쌓인다.
                    유저는 1로 고정이고 보낸 시각은 서버가 채운다.
                    """)
    // @ResponseStatus는 OpenAPI 문서용이다. 없으면 springdoc이 응답을 200으로 적어
    // 실제 상태(201)와 문서가 어긋난다. 런타임 상태는 아래 ResponseEntity가 성공 코드에서 가져온다
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<ApiResponse<RemittanceSaveResponse>> saveRemittance(
            @Valid @RequestBody RemittanceSaveRequest request) {

        // 201을 실제로 내려야 해서 이 엔드포인트만 ResponseEntity로 감싼다.
        // ApiResponse를 그대로 반환하면 성공 코드의 status가 응답에 반영되지 않는다
        return ApiResponse.onSuccessEntity(
                RemittanceSuccessCode.REMITTANCE_SAVED,
                remittanceService.saveRemittance(request));
    }
}
