package com.openforms.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.openforms.common.exception.ConflictException;
import com.openforms.common.exception.UnauthorizedException;
import com.openforms.common.security.JwtTokenProvider;
import com.openforms.user.domain.User;
import com.openforms.user.dto.LoginRequest;
import com.openforms.user.dto.RefreshRequest;
import com.openforms.user.dto.RegisterRequest;
import com.openforms.user.dto.TokenResponse;
import com.openforms.user.dto.UserResponse;
import com.openforms.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 인증 서비스 규칙을 검증합니다. 하이브리드 TDD 로 규칙(이메일 중복 409·잘못된 자격 401)을 먼저 고정합니다.
 *
 * <p>토큰 발급/회전의 세부 규칙은 {@link RefreshTokenServiceTest} 가 담당하고, 여기서는 인증 흐름이
 * 그 결과를 어떻게 조합해 응답하는지(로그인은 두 토큰을 함께, 리프레시는 둘 다 새 값)만 봅니다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenService refreshTokenService;
    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("이미 사용 중인 이메일로 가입하면 409(EMAIL_ALREADY_EXISTS)")
    void registerDuplicateEmail() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("dup@example.com", "password1234", "홍길동")))
                .asInstanceOf(InstanceOfAssertFactories.type(ConflictException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("EMAIL_ALREADY_EXISTS"));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("가입 성공 시 비밀번호는 해싱되어 저장되고(원문 미저장) 사용자 표현을 반환한다")
    void registerSuccessHashesPassword() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1234")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.register(
                new RegisterRequest("new@example.com", "password1234", "새 사용자"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$hashed");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("password1234");
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.name()).isEqualTo("새 사용자");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 401(INVALID_CREDENTIALS)")
    void loginUnknownEmail() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("ghost@example.com", "password1234")))
                .asInstanceOf(InstanceOfAssertFactories.type(UnauthorizedException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401(INVALID_CREDENTIALS) — 존재 여부를 노출하지 않는다")
    void loginWrongPassword() {
        User user = new User("creator@example.com", "$2a$stored", "제작자");
        when(userRepository.findByEmail("creator@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "$2a$stored")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("creator@example.com", "wrong-password")))
                .asInstanceOf(InstanceOfAssertFactories.type(UnauthorizedException.class))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("자격이 일치하면 Bearer 액세스 토큰과 리프레시 토큰을 함께 발급한다")
    void loginSuccessIssuesToken() {
        User user = new User("creator@example.com", "$2a$stored", "제작자");
        when(userRepository.findByEmail("creator@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password1234", "$2a$stored")).thenReturn(true);
        when(jwtTokenProvider.issue("creator@example.com")).thenReturn("jwt-token");
        when(jwtTokenProvider.expiresInSeconds()).thenReturn(3600L);
        when(refreshTokenService.issue(user)).thenReturn(issuedFor(user, "refresh-token"));

        TokenResponse response = authService.login(
                new LoginRequest("creator@example.com", "password1234"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("리프레시하면 회전된 리프레시 토큰과 새 액세스 토큰을 함께 돌려준다")
    void refreshIssuesNewPair() {
        User user = new User("creator@example.com", "$2a$stored", "제작자");
        when(refreshTokenService.rotate("old-refresh")).thenReturn(issuedFor(user, "new-refresh"));
        when(jwtTokenProvider.issue("creator@example.com")).thenReturn("new-jwt");
        when(jwtTokenProvider.expiresInSeconds()).thenReturn(3600L);

        TokenResponse response = authService.refresh(new RefreshRequest("old-refresh"));

        // 액세스만 갱신하고 리프레시를 재사용하면 회전이 무의미해지므로 둘 다 새 값이어야 합니다.
        assertThat(response.accessToken()).isEqualTo("new-jwt");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    @DisplayName("로그아웃은 제시된 리프레시 토큰을 폐기한다(서버 측 무효화)")
    void logoutRevokesRefreshToken() {
        authService.logout(new RefreshRequest("some-refresh"));

        verify(refreshTokenService).revoke("some-refresh");
    }

    private IssuedRefreshToken issuedFor(User user, String token) {
        return new IssuedRefreshToken(user, token, LocalDateTime.now().plusDays(14));
    }
}
