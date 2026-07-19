package com.openforms.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.openforms.common.exception.UnauthorizedException;
import com.openforms.common.security.RefreshTokenProperties;
import com.openforms.user.domain.RefreshToken;
import com.openforms.user.domain.User;
import com.openforms.user.repository.RefreshTokenRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 리프레시 토큰의 규칙을 먼저 고정합니다(하이브리드 TDD 의 red).
 *
 * <p>여기서 못박는 것은 네 가지입니다 — <b>회전</b>(쓴 토큰은 즉시 폐기되고 새 토큰이 나온다),
 * <b>재사용 탐지</b>(폐기된 토큰이 다시 오면 사본 유출로 보고 그 사용자 전부를 폐기한다),
 * <b>실패 응답의 무차별성</b>(없음·만료·폐기를 모두 같은 401 로 답해 어느 쪽인지 노출하지 않는다),
 * <b>로그아웃의 멱등성</b>입니다.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final int EXPIRATION_DAYS = 14;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private final User user = new User("creator@example.com", "$2a$stored", "제작자");

    private RefreshTokenService service() {
        return new RefreshTokenService(refreshTokenRepository,
                new RefreshTokenProperties(EXPIRATION_DAYS));
    }

    @Test
    @DisplayName("발급 시 원문은 반환하되 저장은 해시로만 한다(원문 미보관)")
    void issueStoresOnlyHash() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        IssuedRefreshToken issued = service().issue(user);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertThat(issued.token()).isNotBlank();
        // 저장된 값은 원문이 아니라 SHA-256 hex(64자)입니다.
        assertThat(saved.getValue().getTokenHash()).isNotEqualTo(issued.token())
                .hasSize(64).matches("[0-9a-f]{64}");
        assertThat(issued.expiresAt()).isAfter(LocalDateTime.now().plusDays(EXPIRATION_DAYS - 1));
    }

    @Test
    @DisplayName("발급할 때마다 서로 다른 토큰이 나온다")
    void issueGeneratesUniqueTokens() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        RefreshTokenService service = service();

        assertThat(service.issue(user).token()).isNotEqualTo(service.issue(user).token());
    }

    @Test
    @DisplayName("유효한 토큰으로 회전하면 기존 토큰은 폐기되고 새 토큰이 발급된다")
    void rotateRevokesOldAndIssuesNew() {
        RefreshTokenService service = service();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        IssuedRefreshToken original = service.issue(user);
        RefreshToken stored = storedTokenOf(original);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        IssuedRefreshToken rotated = service.rotate(original.token());

        assertThat(stored.isRevoked()).isTrue();
        assertThat(rotated.token()).isNotEqualTo(original.token());
        assertThat(rotated.user()).isSameAs(user);
    }

    @Test
    @DisplayName("존재하지 않는 토큰으로 회전하면 401(INVALID_REFRESH_TOKEN)")
    void rotateUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().rotate("unknown-token"))
                .asInstanceOf(InstanceOfAssertFactories.type(UnauthorizedException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("만료된 토큰으로 회전하면 401 — 없는 토큰과 같은 코드로 답한다")
    void rotateExpiredToken() {
        LocalDateTime past = LocalDateTime.now().minusDays(20);
        RefreshToken expired = new RefreshToken(user, "c".repeat(64), past, past.plusDays(1));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service().rotate("expired-token"))
                .asInstanceOf(InstanceOfAssertFactories.type(UnauthorizedException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("INVALID_REFRESH_TOKEN"));

        // 만료는 정상적인 수명 종료이므로 다른 세션까지 끊지 않습니다.
        verify(refreshTokenRepository, never()).revokeAllByUserId(any(), any());
    }

    @Test
    @DisplayName("이미 회전된 토큰이 다시 오면 유출로 보고 그 사용자의 토큰을 전부 폐기한다")
    void rotateDetectsReuse() {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken used = new RefreshToken(user, "d".repeat(64), now, now.plusDays(14));
        used.revoke(now);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service().rotate("already-rotated"))
                .asInstanceOf(InstanceOfAssertFactories.type(UnauthorizedException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("INVALID_REFRESH_TOKEN"));

        verify(refreshTokenRepository).revokeAllByUserId(eq(user.getId()), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("로그아웃하면 해당 토큰이 폐기되어 이후 회전이 401 이 된다")
    void revokeMakesTokenUnusable() {
        RefreshTokenService service = service();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        IssuedRefreshToken issued = service.issue(user);
        RefreshToken stored = storedTokenOf(issued);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        service.revoke(issued.token());

        assertThat(stored.isRevoked()).isTrue();
        assertThatThrownBy(() -> service.rotate(issued.token()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("이미 없거나 무효한 토큰으로 로그아웃해도 예외를 던지지 않는다(멱등)")
    void revokeIsIdempotent() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatCode(() -> service().revoke("gone")).doesNotThrowAnyException();
    }

    /** 발급 호출 때 저장소로 넘어간 엔티티(해시가 담긴 실제 저장 대상)를 꺼냅니다. */
    private RefreshToken storedTokenOf(IssuedRefreshToken issued) {
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
        return saved.getAllValues().getLast();
    }
}
