package com.openforms.common.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * 액세스 토큰(JWT)의 발급·검증을 담당합니다(jjwt, HS256).
 *
 * <p>무상태 인증이므로 토큰 자체가 신뢰 원천입니다. subject 에는 사용자 식별자로 이메일을 담아,
 * 인증 주체 이름(감사 {@code created_by}·API 호출 이력 principal)과 일관되게 합니다. 매 요청마다
 * DB 를 조회하지 않고 서명 검증만으로 인증을 구성합니다.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = properties.expirationSeconds();
    }

    /** subject(이메일)로 서명된 액세스 토큰을 발급합니다. */
    public String issue(String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key)
                .compact();
    }

    /** 토큰의 subject(이메일)를 반환합니다. 서명·만료가 유효하지 않으면 {@link JwtException} 을 던집니다. */
    public String parseSubject(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    /** 서명·만료·형식이 모두 유효하면 true 입니다. 신뢰할 수 없는 입력이므로 어떤 파싱 실패든 무효로 봅니다. */
    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** 토큰 만료까지의 초입니다(로그인 응답의 {@code expiresIn} 용). */
    public long expiresInSeconds() {
        return expirationSeconds;
    }
}
