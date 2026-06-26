package com.example.team3plusspring.global.exception;

import com.example.team3plusspring.global.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 서비스 계층에서 BusinessException을 던질 때 사용하는 에러
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }

    // @Valid 요청 body 검증에 실패했을 때 사용하는 에러
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        List<String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .toList();

        return ResponseEntity
                .status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED, errors));
    }

    // @Validated 쿼리 파라미터, 경로 변수 검증에 실패했을 때 사용하는 에러
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleConstraintViolationException(
            ConstraintViolationException exception
    ) {
        List<String> errors = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getMessage())
                .toList();

        return ResponseEntity
                .status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED, errors));
    }

    // 필수 쿼리 파라미터가 누락됐을 때 사용하는 에러
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException() {
        return ResponseEntity
                .status(ErrorCode.MISSING_REQUIRED_FIELD.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.MISSING_REQUIRED_FIELD));
    }

    // 쿼리 파라미터나 경로 변수 타입 변환에 실패했을 때 사용하는 에러
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception
    ) {
        ErrorCode errorCode = isEnumType(exception) ? ErrorCode.INVALID_ENUM_VALUE : ErrorCode.VALIDATION_FAILED;

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }

    // JSON 문법 오류처럼 요청 body를 읽을 수 없을 때 사용하는 에러
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception
    ) {
        ErrorCode errorCode = isEnumFormatException(exception) ? ErrorCode.INVALID_ENUM_VALUE : ErrorCode.VALIDATION_FAILED;

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }

    // 지원하지 않는 HTTP method로 요청했을 때 사용하는 에러
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException() {
        return ResponseEntity
                .status(ErrorCode.METHOD_NOT_ALLOWED.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDeniedException() {
        return ResponseEntity
                .status(ErrorCode.FORBIDDEN.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.FORBIDDEN));
    }

    // 위에서 처리하지 못한 예상 밖의 서버 예외가 발생했을 때 사용하는 에러
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("예상하지 못한 예외가 발생했습니다.", e);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    // Enum 타입 변환 실패 여부를 확인하는 메서드
    private boolean isEnumType(MethodArgumentTypeMismatchException exception) {
        Class<?> requiredType = exception.getRequiredType();
        return requiredType != null && requiredType.isEnum();
    }

    private boolean isEnumFormatException(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof InvalidFormatException invalidFormatException) {
            Class<?> targetType = invalidFormatException.getTargetType();
            return targetType != null && targetType.isEnum();
        }

        return false;
    }
}
