package com.openforms.user.controller;

import com.openforms.common.exception.ErrorResponse;
import com.openforms.common.openapi.AuthenticatedErrors;
import com.openforms.common.openapi.ValidationErrors;
import com.openforms.user.dto.LoginRequest;
import com.openforms.user.dto.RegisterRequest;
import com.openforms.user.dto.TokenResponse;
import com.openforms.user.dto.UserResponse;
import com.openforms.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "인증", description = "회원가입·로그인·본인 조회")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 회원가입 → 201. */
    @Operation(summary = "회원가입", description = "이메일·비밀번호·이름으로 제작자 계정을 생성합니다(익명 허용).")
    @ApiResponse(responseCode = "201", description = "생성됨")
    @ValidationErrors
    @ApiResponse(responseCode = "409", description = "이미 가입된 이메일 (EMAIL_ALREADY_EXISTS)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /** 로그인 → 200 + 액세스 토큰. */
    @Operation(summary = "로그인", description = "자격이 일치하면 Bearer 액세스 토큰을 발급합니다(익명 허용).")
    @ApiResponse(responseCode = "200", description = "액세스 토큰 발급")
    @ValidationErrors
    @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치 (INVALID_CREDENTIALS)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** 현재 인증 주체 조회 → 200. principal(이메일)은 JWT 필터가 주입합니다. */
    @Operation(summary = "본인 조회", description = "토큰의 인증 주체에 해당하는 사용자 정보를 반환합니다(인증 필요).")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "본인 정보")
    @AuthenticatedErrors
    @ApiResponse(responseCode = "404", description = "토큰은 유효하나 주체가 존재하지 않음 (USER_NOT_FOUND)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
