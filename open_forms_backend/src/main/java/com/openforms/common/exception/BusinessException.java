package com.openforms.common.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 규칙 위반을 나타내는 예외의 공통 상위 타입입니다.
 *
 * <p>상태 코드는 하위 타입이 고정하고, 구체적인 {@code code} 문자열과 메시지는 예외를 던지는 각
 * 기능이 제공합니다. 이렇게 하면 {@code common} 이 개별 도메인의 에러 코드를 알 필요가 없어
 * 기능 간 경계(모듈러 모놀리스)가 유지됩니다.
 */
public abstract class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<ErrorResponse.FieldError> fieldErrors;

    protected BusinessException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of());
    }

    /**
     * 어떤 항목이 왜 잘못됐는지까지 알려야 할 때 사용합니다(예: 필수 응답이 빠진 질문 목록).
     * {@code @Valid} 실패가 아니어도 동일한 {@code fieldErrors} 포맷으로 답할 수 있게 합니다.
     */
    protected BusinessException(HttpStatus status, String code, String message,
            List<ErrorResponse.FieldError> fieldErrors) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<ErrorResponse.FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
