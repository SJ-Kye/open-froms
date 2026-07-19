package com.openforms.user.service;

import com.openforms.common.exception.UnauthorizedException;
import com.openforms.common.security.RefreshTokenProperties;
import com.openforms.user.domain.RefreshToken;
import com.openforms.user.domain.User;
import com.openforms.user.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리프레시 토큰의 발급·회전·폐기를 담당합니다.
 *
 * <p>토큰은 서명이 아니라 <b>DB 의 상태</b>로 신뢰를 얻습니다. 그래서 값 자체에는 의미를 담지 않고
 * 256비트 난수를 쓰며, 저장은 SHA-256 해시로만 합니다(원문은 발급 응답 이후 어디에도 남지 않습니다).
 *
 * <p>핵심 규칙은 <b>회전</b>입니다. 한 번 쓴 토큰은 즉시 폐기하고 새 토큰을 내주므로 토큰은 1회용이며,
 * 유출된 사본과 정상 클라이언트가 같은 토큰을 쓰면 둘 중 나중 요청이 반드시 실패합니다. 그 실패가
 * 곧 유출 신호이므로, 폐기된 토큰이 다시 오면 그 사용자의 토큰을 모두 무효화합니다.
 */
@Service
@Transactional(readOnly = true)
public class RefreshTokenService {

    /** 토큰 원문의 바이트 길이입니다. 256비트면 추측이 불가능하므로 stretching 없이 해시만 씁니다. */
    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties properties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
            RefreshTokenProperties properties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
    }

    /** 새 리프레시 토큰을 발급합니다. 반환값의 원문은 이 호출에서만 얻을 수 있습니다. */
    @Transactional
    public IssuedRefreshToken issue(User user) {
        String rawToken = generateToken();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(properties.expirationDays());

        refreshTokenRepository.save(new RefreshToken(user, hash(rawToken), now, expiresAt));
        return new IssuedRefreshToken(user, rawToken, expiresAt);
    }

    /**
     * 제시된 토큰을 검증하고 새 토큰으로 회전합니다.
     *
     * <p>실패는 원인을 가리지 않고 모두 같은 401 입니다. "만료됨"과 "폐기됨"을 구분해 알려 주면 공격자가
     * 확보한 토큰이 유효했던 것인지 확인할 수 있게 됩니다.
     */
    @Transactional
    public IssuedRefreshToken rotate(String rawToken) {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(RefreshTokenService::invalidRefreshToken);

        if (stored.isRevoked()) {
            // 이미 회전된 토큰이 다시 왔습니다. 정상 클라이언트는 새 토큰을 받아 갔으므로, 이 요청은
            // 사본을 쥔 쪽입니다. 어느 쪽이 공격자인지 알 수 없으니 전부 끊고 재로그인을 요구합니다.
            User owner = stored.getUser();
            refreshTokenRepository.revokeAllByUserId(owner.getId(), now);
            throw invalidRefreshToken();
        }
        if (!stored.isUsable(now)) {
            // 만료는 정상적인 수명 종료입니다. 유출 신호가 아니므로 다른 세션은 건드리지 않습니다.
            throw invalidRefreshToken();
        }

        stored.revoke(now);
        return issue(stored.getUser());
    }

    /**
     * 제시된 토큰을 폐기합니다(로그아웃).
     *
     * <p>토큰이 없거나 이미 무효여도 조용히 성공합니다. 로그아웃의 목적은 "이 토큰이 더는 쓰이지 않는
     * 상태"이고 그 상태는 이미 달성되어 있으며, 실패로 답하면 토큰의 존재 여부를 알려 주게 됩니다.
     */
    @Transactional
    public void revoke(String rawToken) {
        Optional<RefreshToken> stored = refreshTokenRepository.findByTokenHash(hash(rawToken));
        stored.ifPresent(token -> token.revoke(LocalDateTime.now()));
    }

    /** 의미 없는 256비트 난수를 URL-safe Base64 로 만듭니다(값 자체에 정보를 담지 않습니다). */
    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 저장·조회 키로 쓸 SHA-256 hex 를 계산합니다.
     *
     * <p>비밀번호와 달리 BCrypt 를 쓰지 않는 이유는 <b>해시로 조회</b>해야 하기 때문입니다. BCrypt 는
     * 행마다 salt 가 달라 동등 비교가 불가능해 전 행을 훑어야 합니다. 원문이 고엔트로피 난수라 사전
     * 공격의 대상이 아니므로 stretching 이 주는 이득도 없습니다.
     */
    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 은 모든 JRE 가 제공하도록 규격에 정해져 있어 실제로는 도달하지 않습니다.
            throw new IllegalStateException("SHA-256 을 사용할 수 없습니다.", e);
        }
    }

    /** 없음·만료·폐기를 한 예외로 통일해, 토큰이 어떤 상태였는지 노출하지 않습니다. */
    private static UnauthorizedException invalidRefreshToken() {
        return new UnauthorizedException("INVALID_REFRESH_TOKEN",
                "리프레시 토큰이 유효하지 않습니다. 다시 로그인해 주세요.");
    }
}
