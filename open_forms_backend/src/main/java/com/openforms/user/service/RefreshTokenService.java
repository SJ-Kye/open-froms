package com.openforms.user.service;

import com.openforms.common.security.RefreshTokenProperties;
import com.openforms.user.domain.User;
import com.openforms.user.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

/**
 * 리프레시 토큰의 발급·회전·폐기를 담당합니다. (구현 예정 — 규칙은 {@code RefreshTokenServiceTest} 참고)
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties properties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
            RefreshTokenProperties properties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
    }

    /** 새 리프레시 토큰을 발급합니다. */
    public IssuedRefreshToken issue(User user) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }

    /** 제시된 토큰을 검증하고 새 토큰으로 회전합니다. */
    public IssuedRefreshToken rotate(String rawToken) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }

    /** 제시된 토큰을 폐기합니다(로그아웃). */
    public void revoke(String rawToken) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }
}
