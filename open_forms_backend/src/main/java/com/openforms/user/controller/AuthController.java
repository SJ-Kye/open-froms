package com.openforms.user.controller;

import com.openforms.user.dto.LoginRequest;
import com.openforms.user.dto.RegisterRequest;
import com.openforms.user.dto.TokenResponse;
import com.openforms.user.dto.UserResponse;
import com.openforms.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 엔드포인트입니다. HTTP 매핑·{@code @Valid} 검증·상태 코드 결정만 담당하고, 규칙은 {@link AuthService}
 * 에 위임합니다.
 *
 * <p>{@code /register}·{@code /login} 은 익명 허용({@code SecurityConfig} 의 {@code /api/auth/**}),
 * {@code /me} 는 인증이 필요합니다(토큰 없으면 필터 단에서 401).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 회원가입 → 201. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /** 로그인 → 200 + 액세스 토큰. */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** 현재 인증 주체 조회 → 200. principal(이메일)은 JWT 필터가 주입합니다. */
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
