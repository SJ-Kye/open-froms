package com.openforms.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 자격 증명이 유효하지 않을 때 던집니다(401). 예: 로그인 시 이메일/비밀번호 불일치
 * ({@code "INVALID_CREDENTIALS"}). 구체 코드는 기능이 전달합니다.
 *
 * <p>필터 단계의 미인증(토큰 부재·만료)은 {@code AuthenticationEntryPoint} 가 처리하지만, 로그인처럼
 * 컨트롤러 흐름에서 발생하는 401 은 이 예외로 {@link GlobalExceptionHandler} 를 거쳐 동일한 공통 포맷으로
 * 반환합니다.
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }
}
