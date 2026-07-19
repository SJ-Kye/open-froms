package com.openforms.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 본문입니다. 컨트롤러의 {@code @Valid} 로 형식을 검증합니다.
 *
 * @param email    로그인 식별자이자 인증 주체 이름(감사 {@code created_by}·API 이력 principal)으로 쓰입니다.
 * @param password 평문 비밀번호. 서비스에서 BCrypt 로 해싱해 저장하며 원문은 보관하지 않습니다.
 * @param name     표시 이름입니다.
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 100) String name) {
}
