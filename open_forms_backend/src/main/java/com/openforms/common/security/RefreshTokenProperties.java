package com.openforms.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 리프레시 토큰 설정입니다(application.yml 의 {@code app.refresh-token.*}).
 *
 * <p>{@link JwtProperties} 와 나눠 둔 이유는 리프레시 토큰이 JWT 가 아니기 때문입니다. 서명으로 자신을
 * 증명하는 액세스 토큰과 달리, 리프레시 토큰은 DB 에 상태로 존재하는 불투명(opaque) 난수입니다.
 *
 * @param expirationDays 리프레시 토큰 유효 기간(일). 이 기간이 지나면 재로그인이 필요합니다.
 */
@ConfigurationProperties(prefix = "app.refresh-token")
public record RefreshTokenProperties(int expirationDays) {
}
