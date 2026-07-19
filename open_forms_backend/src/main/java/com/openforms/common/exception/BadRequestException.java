package com.openforms.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 요청 값이 도메인 규칙을 위반해 처리할 수 없을 때 던집니다(400). 필드 형식 검증({@code @Valid} → 400
 * {@code VALIDATION_FAILED})과 달리, <b>타입에 따라 달라지는 교차 규칙</b>처럼 애너테이션으로 표현하기
 * 어려운 검증을 담당합니다. 예: 선택형 질문의 선택지 부족({@code "OPTIONS_REQUIRED"}),
 * 범위 역전({@code "INVALID_VALUE_RANGE"}). 구체 코드는 기능이 전달합니다.
 */
public class BadRequestException extends BusinessException {

    public BadRequestException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
}
