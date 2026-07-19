package com.openforms.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 모든 예외를 한 곳에서 공통 에러 포맷({@link ErrorResponse})으로 변환합니다(코드 곳곳에 산발 금지).
 *
 * <p>필터 단계에서 발생하는 미인증(401)과 필터 단 인가 실패(403)의 JSON 통일은 스프링 시큐리티의
 * {@code AuthenticationEntryPoint}/{@code AccessDeniedHandler} 몫이라 보안 설정 작업에서 다룹니다.
 * 여기서는 서비스/컨트롤러가 던진 {@link AccessDeniedException}(비소유자 접근)까지 처리합니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 요청 본문 {@code @Valid} 검증 실패 → 400, 필드별 사유 포함. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "요청 값이 올바르지 않습니다.", request,
                fieldErrors);
    }

    /** 요청 본문 JSON 파싱 실패 → 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "요청 본문을 해석할 수 없습니다.", request,
                List.of());
    }

    /** 매핑된 핸들러가 없는 경로(오탈자 URL 등) → 404. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex,
            HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.", request,
                List.of());
    }

    /** 도메인 규칙 위반 → 예외가 지닌 상태/코드로 변환(404·409 등). */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), request, List.of());
    }

    /** 서비스/컨트롤러가 던진 인가 실패(비소유자) → 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "접근 권한이 없습니다.", request, List.of());
    }

    /** 미처리 예외 → 500. 내부 메시지는 노출하지 않고 서버 로그에만 남깁니다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("처리되지 않은 예외: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.", request,
                List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
            HttpServletRequest request, List<ErrorResponse.FieldError> fieldErrors) {
        ErrorResponse body = ErrorResponse.of(status, code, message, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
