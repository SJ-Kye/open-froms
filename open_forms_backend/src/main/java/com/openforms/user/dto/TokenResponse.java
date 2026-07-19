package com.openforms.user.dto;

/**
 * 로그인·리프레시 성공 응답입니다. 클라이언트는 {@code accessToken} 을 {@code Authorization: Bearer}
 * 로 첨부하고, 만료되면 {@code refreshToken} 으로 갱신합니다.
 *
 * <p>{@code expiresIn} 은 액세스 토큰 기준입니다. 리프레시 토큰의 만료는 응답에 담지 않습니다 —
 * 만료되면 갱신 시도가 401 로 알려 주므로 클라이언트가 그 값으로 내릴 판단이 없고, 노출하면 토큰
 * 정책이 클라이언트 코드에 복제됩니다.
 *
 * @param accessToken  서명된 JWT 액세스 토큰입니다.
 * @param refreshToken 액세스 토큰 갱신에 쓰는 불투명 토큰입니다(사용 시 회전되는 1회용).
 * @param tokenType    토큰 유형이며 항상 {@code "Bearer"} 입니다.
 * @param expiresIn    액세스 토큰 만료까지 남은 초입니다.
 */
public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {

    /** 표준 Bearer 토큰 응답을 만듭니다. */
    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
