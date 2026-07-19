package com.openforms.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openforms.user.dto.LoginRequest;
import com.openforms.user.dto.RegisterRequest;
import com.openforms.user.dto.TokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
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
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber());
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

    // --- helpers ---

    private void register(String email, String password, String name) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(email, password, name))))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, TokenResponse.class).accessToken();
    }

    private String json(Object value) {
        return objectMapper.writeValueAsString(value);
    }
}
