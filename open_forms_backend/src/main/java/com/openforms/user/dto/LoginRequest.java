package com.openforms.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 본문입니다. 자격 대조는 서비스가 수행하므로 여기서는 빈 값만 거릅니다(형식 세부 검증 불필요).
 *
 * @param email    가입 시 사용한 이메일입니다.
 * @param password 평문 비밀번호입니다.
 */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password) {
}
