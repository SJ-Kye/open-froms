package com.openforms.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 요청한 리소스가 없을 때 던집니다(404). 구체 코드는 기능이 전달합니다(예: {@code "FORM_NOT_FOUND"}).
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }
}
