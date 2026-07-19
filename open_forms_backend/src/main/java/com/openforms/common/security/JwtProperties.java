package com.openforms.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정입니다(application.yml 의 {@code app.jwt.*}).
 *
 * @param secret HS256 서명 키. 32바이트(256비트) 이상이어야 합니다.
 * @param expirationSeconds 액세스 토큰 유효 기간(초).
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationSeconds) {
}
