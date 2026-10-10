package com.ceos24.mashup2backend.domain.receivemethod.controller;

import com.ceos24.mashup2backend.domain.receivemethod.dto.request.ReceiveOptionRequest;
import com.ceos24.mashup2backend.domain.receivemethod.dto.response.ReceiveOptionListResponse;
import com.ceos24.mashup2backend.domain.receivemethod.exception.code.ReceiveOptionSuccessCode;
import com.ceos24.mashup2backend.domain.receivemethod.service.ReceiveOptionService;
import com.ceos24.mashup2backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/receive-options")
@RequiredArgsConstructor
@Tag(name = "수취 옵션", description = "받는 방법 리스트 조회")
public class ReceiveOptionController {

    private final ReceiveOptionService receiveOptionService;

    @Operation(
            summary = "받는 방법 리스트 조회",
            description = """
                    받는 통화로 수취 가능한 방법 목록을 정렬 기준에 따라 조회한다.
                    보내는 통화는 KRW만 지원하고, 정렬 기준을 지정하지 않으면 RATE(수령액 내림차순)로 동작한다.
                    선택 불가 옵션은 정렬 기준과 무관하게 항상 목록 맨 뒤에 온다.
                    """)
    @GetMapping
    public ApiResponse<ReceiveOptionListResponse> getReceiveOptions(
            @ParameterObject @Valid @ModelAttribute ReceiveOptionRequest request) {

        return ApiResponse.onSuccess(
                ReceiveOptionSuccessCode.RECEIVE_OPTION_LIST_OK,
                receiveOptionService.getReceiveOptions(request));
    }
}
