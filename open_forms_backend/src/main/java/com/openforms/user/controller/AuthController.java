package com.openforms.user.controller;

import com.openforms.common.exception.ErrorResponse;
import com.openforms.common.openapi.AuthenticatedErrors;
import com.openforms.common.openapi.ValidationErrors;
import com.openforms.user.dto.LoginRequest;
import com.openforms.user.dto.RefreshRequest;
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
 * <p>{@code /register}·{@code /login}·{@code /refresh}·{@code /logout} 은 익명 허용이고,
 * {@code /me} 는 인증이 필요합니다(토큰 없으면 필터 단에서 401). 갱신·로그아웃이 익명 허용인 것은
 * 두 API 의 자격증명이 액세스 토큰이 아니라 <b>리프레시 토큰 자체</b>이기 때문입니다 — 액세스 토큰
 * 인증을 걸면 만료 후에는 갱신할 수 없어 목적이 무너집니다.
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

    /** 리프레시 토큰 회전 → 200 + 새 토큰 쌍. */
    @Operation(summary = "토큰 갱신",
            description = "리프레시 토큰을 회전시켜 새 액세스·리프레시 토큰을 발급합니다. "
                    + "액세스 토큰은 이미 만료되었을 수 있으므로 요구하지 않습니다(익명 허용) — "
                    + "이 API 의 자격증명은 리프레시 토큰 자체입니다.")
    @ApiResponse(responseCode = "200", description = "새 토큰 쌍 발급")
    @ValidationErrors
    @ApiResponse(responseCode = "401",
            description = "리프레시 토큰이 없음·만료됨·이미 폐기됨 (INVALID_REFRESH_TOKEN)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    /** 로그아웃 → 204. 이미 무효한 토큰이어도 204 입니다(멱등). */
    @Operation(summary = "로그아웃",
            description = "리프레시 토큰을 서버에서 폐기합니다. 이미 없거나 무효한 토큰이어도 204 로 "
                    + "답합니다 — 목적하는 상태가 이미 달성되어 있고, 실패로 답하면 토큰의 존재 여부를 "
                    + "알려 주게 되기 때문입니다. 액세스 토큰은 무상태라 만료 전까지 유효하므로 "
                    + "클라이언트가 함께 폐기해야 합니다.")
    @ApiResponse(responseCode = "204", description = "폐기됨")
    @ValidationErrors
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
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
