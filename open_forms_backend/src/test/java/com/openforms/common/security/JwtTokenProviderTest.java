package com.openforms.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JwtTokenProvider 의 발급·검증을 검증합니다(서명·만료·변조).
 */
class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long-000";

    private final JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, 3600));

    @Test
    @DisplayName("발급한 토큰은 유효하며 subject(이메일)를 되돌려준다")
    void issueAndParse() {
        String token = provider.issue("creator@example.com");

        assertThat(provider.isValid(token)).isTrue();
        assertThat(provider.parseSubject(token)).isEqualTo("creator@example.com");
    }

    @Test
    @DisplayName("변조된 토큰(페이로드 조작)은 서명 불일치로 유효하지 않다")
    void tamperedIsInvalid() {
        String token = provider.issue("creator@example.com");
        String[] parts = token.split("\\.");
        // 페이로드 첫 글자를 바꿔 서명 대상 내용을 변조하면 서명이 맞지 않는다.
        char first = parts[1].charAt(0);
        String tamperedPayload = (first == 'A' ? 'B' : 'A') + parts[1].substring(1);
        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThat(provider.isValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 유효하지 않다")
    void expiredIsInvalid() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(new JwtProperties(SECRET, -60));
        String expired = expiredProvider.issue("creator@example.com");

        assertThat(expiredProvider.isValid(expired)).isFalse();
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 유효하지 않다")
    void differentKeyIsInvalid() {
        String token = provider.issue("creator@example.com");
        JwtTokenProvider other = new JwtTokenProvider(
                new JwtProperties("another-secret-key-also-at-least-32-bytes-xx", 3600));

        assertThat(other.isValid(token)).isFalse();
    }
}
