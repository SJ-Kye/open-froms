package com.openforms.user.service;

import com.openforms.common.exception.ConflictException;
import com.openforms.common.exception.ResourceNotFoundException;
import com.openforms.common.exception.UnauthorizedException;
import com.openforms.common.security.JwtTokenProvider;
import com.openforms.user.domain.User;
import com.openforms.user.dto.LoginRequest;
import com.openforms.user.dto.RefreshRequest;
import com.openforms.user.dto.RegisterRequest;
import com.openforms.user.dto.TokenResponse;
import com.openforms.user.dto.UserResponse;
import com.openforms.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 규칙의 단일 지점입니다(회원가입·로그인·본인 조회). 소유권/상태 전이 같은 비즈니스 규칙은 서비스에만
 * 두고, 컨트롤러는 HTTP 매핑만 담당합니다.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    /** 이메일 중복이면 409, 아니면 비밀번호를 해싱해 저장하고 표현을 반환합니다. */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다.");
        }
        String passwordHash = passwordEncoder.encode(request.password());
        User user = userRepository.save(new User(request.email(), passwordHash, request.name()));
        return UserResponse.from(user);
    }

    /**
     * 자격이 일치하면 액세스 토큰과 리프레시 토큰을 함께 발급합니다. 불일치는 401(사용자 존재 여부 미노출).
     *
     * <p>리프레시 토큰 저장이 있으므로 쓰기 트랜잭션입니다.
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(AuthService::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return issueTokens(refreshTokenService.issue(user));
    }

    /**
     * 리프레시 토큰을 회전시키고 새 토큰 쌍을 발급합니다.
     *
     * <p>액세스 토큰은 만료되었을 수 있으므로 요구하지 않습니다. 이 API 의 자격증명은 리프레시 토큰
     * 자체이며, 유효성 판정은 {@link RefreshTokenService#rotate(String)} 가 담당합니다.
     */
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        return issueTokens(refreshTokenService.rotate(request.refreshToken()));
    }

    /** 제시된 리프레시 토큰을 폐기합니다(로그아웃). 이미 무효한 토큰이어도 성공으로 답합니다. */
    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    /** 방금 발급/회전된 리프레시 토큰의 주인에게 액세스 토큰을 붙여 응답을 만듭니다. */
    private TokenResponse issueTokens(IssuedRefreshToken refreshToken) {
        String accessToken = jwtTokenProvider.issue(refreshToken.user().getEmail());
        return TokenResponse.bearer(accessToken, refreshToken.token(),
                jwtTokenProvider.expiresInSeconds());
    }

    /** 인증 주체(이메일)에 해당하는 사용자 표현을 반환합니다. */
    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
        return UserResponse.from(user);
    }

    /**
     * 이메일 미존재와 비밀번호 불일치를 동일 예외로 통일해, 어느 쪽인지(계정 존재 여부)를 노출하지 않습니다.
     */
    private static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
