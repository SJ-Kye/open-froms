package com.openforms.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 현재 상태와 충돌하는 요청일 때 던집니다(409). 예: 이메일 중복({@code "EMAIL_ALREADY_EXISTS"}),
 * 종료된 폼에 대한 응답 제출({@code "FORM_CLOSED"}). 구체 코드는 기능이 전달합니다.
 */
public class ConflictException extends BusinessException {

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
