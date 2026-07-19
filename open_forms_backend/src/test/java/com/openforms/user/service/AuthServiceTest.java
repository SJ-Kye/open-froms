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
import com.openforms.user.dto.RegisterRequest;
import com.openforms.user.dto.TokenResponse;
import com.openforms.user.dto.UserResponse;
import com.openforms.user.repository.UserRepository;
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
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
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
    @DisplayName("자격이 일치하면 Bearer 액세스 토큰을 발급한다")
    void loginSuccessIssuesToken() {
        User user = new User("creator@example.com", "$2a$stored", "제작자");
        when(userRepository.findByEmail("creator@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password1234", "$2a$stored")).thenReturn(true);
        when(jwtTokenProvider.issue("creator@example.com")).thenReturn("jwt-token");
        when(jwtTokenProvider.expiresInSeconds()).thenReturn(3600L);

        TokenResponse response = authService.login(
                new LoginRequest("creator@example.com", "password1234"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }
}
