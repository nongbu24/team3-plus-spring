package com.example.team3plusspring.global.response;


import com.example.team3plusspring.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"status", "code", "message", "data"})
public class ApiResponse<T> {

    private static final String DEFAULT_SUCCESS_MESSAGE = "요청이 성공했습니다.";

    private final int status;
    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(int status, String code, String message, T data) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(HttpStatus.OK, DEFAULT_SUCCESS_MESSAGE, data);
    }

    public static <T> ApiResponse<T> success(HttpStatus status, T data) {
        return success(status, DEFAULT_SUCCESS_MESSAGE, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return success(HttpStatus.OK, message, data);
    }

    public static <T> ApiResponse<T> success(HttpStatus status, String message, T data) {
        return new ApiResponse<>(status.value(), null, message, data);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getHttpStatus().value(), errorCode.name(), errorCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, T data) {
        return new ApiResponse<>(errorCode.getHttpStatus().value(), errorCode.name(), errorCode.getMessage(), data);
    }
}
