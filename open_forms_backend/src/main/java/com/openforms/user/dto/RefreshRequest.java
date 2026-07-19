package com.openforms.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 리프레시·로그아웃 요청 본문입니다. 두 API 모두 "리프레시 토큰 자체"를 자격증명으로 받으므로 본문이
 * 같습니다.
 *
 * @param refreshToken 로그인·직전 리프레시에서 발급받은 토큰 원문입니다.
 */
public record RefreshRequest(@NotBlank String refreshToken) {
}
