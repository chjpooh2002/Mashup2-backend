package com.ceos24.mashup2backend.global.apipayload;

import com.ceos24.mashup2backend.global.apipayload.code.BaseErrorCode;
import com.ceos24.mashup2backend.global.apipayload.code.BaseSuccessCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

@Getter
@RequiredArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponse<T> {

    @JsonProperty("isSuccess")
    private final Boolean isSuccess;

    @JsonProperty("code")
    private final String code;

    @JsonProperty("message")
    private final String message;

    @JsonProperty("result")
    private final T result;

    // 결과 데이터가 있는 성공 응답용 오버로딩 메서드
    public static <T> ApiResponse<T> onSuccess(BaseSuccessCode code, T result){
        return new ApiResponse<>(true, code.getCode(), code.getMessage(), result);
    }

    // 결과 데이터가 없는 성공 응답용 오버로딩 메서드
    public static ApiResponse<Void> onSuccess(BaseSuccessCode code){
        return new ApiResponse<>(true, code.getCode(), code.getMessage(), null);
    }

    // 200이 아닌 상태 코드를 내려야 하는 성공 응답용 (ex. 201 Created).
    // 상태를 컨트롤러에 박지 않고 성공 코드 enum의 status에서 가져온다
    public static <T> ResponseEntity<ApiResponse<T>> onSuccessEntity(BaseSuccessCode code, T result){
        return ResponseEntity.status(code.getStatus()).body(onSuccess(code, result));
    }

    // 결과 데이터가 없는, 200이 아닌 성공 응답용 오버로딩 메서드
    public static ResponseEntity<ApiResponse<Void>> onSuccessEntity(BaseSuccessCode code){
        return ResponseEntity.status(code.getStatus()).body(onSuccess(code));
    }

    // 결과 데이터가 있는 실패 응답용 오버로딩 메서드
    public static <T> ApiResponse<T> onFailure(BaseErrorCode code, T result){
        return new ApiResponse<>(false, code.getCode(), code.getMessage(), result);
    }

    // 결과 데이터가 없는 실패 응답용 오버로딩 메서드
    public static ApiResponse<Void> onFailure(BaseErrorCode code){
        return new ApiResponse<>(false, code.getCode(), code.getMessage(), null);
    }
}
