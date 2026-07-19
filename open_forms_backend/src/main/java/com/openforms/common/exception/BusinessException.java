package com.openforms.common.exception;

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

    protected BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
