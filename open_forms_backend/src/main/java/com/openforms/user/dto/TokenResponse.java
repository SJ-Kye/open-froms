package com.openforms.user.dto;

/**
 * 로그인 성공 응답입니다. 클라이언트는 {@code accessToken} 을 {@code Authorization: Bearer} 로 첨부합니다.
 *
 * @param accessToken 서명된 JWT 액세스 토큰입니다.
 * @param tokenType   토큰 유형이며 항상 {@code "Bearer"} 입니다.
 * @param expiresIn   만료까지 남은 초입니다.
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    /** 표준 Bearer 토큰 응답을 만듭니다. */
    public static TokenResponse bearer(String accessToken, long expiresIn) {
        return new TokenResponse(accessToken, "Bearer", expiresIn);
    }
}
