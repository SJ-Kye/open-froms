package com.openforms.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openforms.common.security.JwtTokenProvider;
import com.openforms.user.dto.LoginRequest;
import com.openforms.user.dto.RefreshRequest;
import com.openforms.user.dto.RegisterRequest;
import com.openforms.user.dto.TokenResponse;
import com.openforms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * 인증 엔드포인트를 실제 보안 체인·필터와 함께 검증합니다(슬라이스 대신 전체 컨텍스트).
 *
 * <p>{@code @Transactional} 로 각 테스트 후 롤백해 회원 데이터가 서로 간섭하지 않게 합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("회원가입 성공 → 201 + 사용자 표현(비밀번호 해시 미포함)")
    void registerCreated() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("new@example.com", "password1234", "홍길동"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("이미 사용 중인 이메일 → 409 EMAIL_ALREADY_EXISTS")
    void registerDuplicateConflict() throws Exception {
        register("dup@example.com", "password1234", "홍길동");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("dup@example.com", "password1234", "다른 이름"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("검증 실패(빈 이메일·짧은 비밀번호) → 400 VALIDATION_FAILED + fieldErrors")
    void registerValidationFails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("", "short", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    @DisplayName("로그인 성공 → 200 + Bearer 액세스 토큰")
    void loginOk() throws Exception {
        register("creator@example.com", "password1234", "제작자");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("creator@example.com", "password1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    @DisplayName("리프레시 → 200 + 액세스·리프레시 모두 새 값 (익명 허용)")
    void refreshRotatesBothTokens() throws Exception {
        register("creator@example.com", "password1234", "제작자");
        TokenResponse issued = loginTokens("creator@example.com", "password1234");

        String body = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshRequest(issued.refreshToken()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        TokenResponse rotated = objectMapper.readValue(body, TokenResponse.class);
        // 리프레시를 재사용하면 회전이 무의미하므로 둘 다 새 값이어야 합니다.
        assertThat(rotated.refreshToken()).isNotEqualTo(issued.refreshToken());
        assertThat(rotated.accessToken()).isNotBlank();
    }

    @Test
    @DisplayName("이미 회전된 리프레시 토큰 재사용 → 401 INVALID_REFRESH_TOKEN")
    void refreshWithRotatedTokenFails() throws Exception {
        register("creator@example.com", "password1234", "제작자");
        TokenResponse issued = loginTokens("creator@example.com", "password1234");
        refresh(issued.refreshToken()).andExpect(status().isOk());

        refresh(issued.refreshToken())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("존재하지 않는 리프레시 토큰 → 401(없음·폐기를 구분하지 않음)")
    void refreshWithUnknownTokenFails() throws Exception {
        refresh("not-a-real-token")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("빈 리프레시 토큰 → 400 VALIDATION_FAILED")
    void refreshWithBlankTokenFails() throws Exception {
        refresh("  ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("로그아웃 → 204, 이후 그 토큰으로 리프레시하면 401")
    void logoutRevokesRefreshToken() throws Exception {
        register("creator@example.com", "password1234", "제작자");
        TokenResponse issued = loginTokens("creator@example.com", "password1234");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshRequest(issued.refreshToken()))))
                .andExpect(status().isNoContent());

        refresh(issued.refreshToken())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("이미 무효한 토큰으로 로그아웃해도 204 (멱등 — 존재 여부를 노출하지 않음)")
    void logoutIsIdempotent() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshRequest("already-gone"))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("잘못된 비밀번호 로그인 → 401 INVALID_CREDENTIALS")
    void loginInvalidCredentials() throws Exception {
        register("creator@example.com", "password1234", "제작자");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("creator@example.com", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("토큰 없이 /me → 401(필터 단 공통 포맷)")
    void meWithoutTokenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("유효 토큰으로 /me → 200 + 본인 이메일")
    void meWithTokenOk() throws Exception {
        register("creator@example.com", "password1234", "제작자");
        String token = login("creator@example.com", "password1234");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("creator@example.com"))
                .andExpect(jsonPath("$.name").value("제작자"));
    }

    @Test
    @DisplayName("토큰은 유효하나 주체가 사라짐 → 404 USER_NOT_FOUND")
    void meWithTokenOfDeletedUser() throws Exception {
        register("ghost@example.com", "password1234", "탈퇴자");
        // 로그인 대신 액세스 토큰을 직접 발급합니다. 로그인은 리프레시 토큰까지 저장하는데, 같은
        // 트랜잭션에서 그 주인을 지우면 영속성 컨텍스트가 먼저 걸려 검증하려는 상황(토큰만 살아남음)에
        // 도달하지 못합니다. 여기서 보려는 것은 액세스 토큰의 무상태성이므로 그 부분만 만듭니다.
        String token = jwtTokenProvider.issue("ghost@example.com");

        // 무상태 JWT 는 발급 후 서버가 폐기할 수 없으므로, 사용자가 사라져도 토큰 자체는 계속 유효합니다.
        // 즉 "인증은 통과했으나 주체가 없는" 상태가 실제로 존재하며, 401 이 아니라 404 로 답합니다.
        userRepository.delete(userRepository.findByEmail("ghost@example.com").orElseThrow());
        userRepository.flush();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    // --- helpers ---

    private void register(String email, String password, String name) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(email, password, name))))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        return loginTokens(email, password).accessToken();
    }

    private TokenResponse loginTokens(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, TokenResponse.class);
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new RefreshRequest(refreshToken))));
    }

    private String json(Object value) {
        return objectMapper.writeValueAsString(value);
    }
}
