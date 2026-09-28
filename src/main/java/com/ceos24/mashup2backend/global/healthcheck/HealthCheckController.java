package com.ceos24.mashup2backend.global.healthcheck;

import com.ceos24.mashup2backend.global.apipayload.ApiResponse;
import com.ceos24.mashup2backend.global.apipayload.code.GeneralSuccessCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthCheckController {

    @GetMapping("/api/health")
    public ApiResponse<Void> healthCheck(){
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }
}
