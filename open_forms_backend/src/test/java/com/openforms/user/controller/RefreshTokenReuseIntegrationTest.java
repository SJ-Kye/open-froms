package com.openforms.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openforms.user.dto.LoginRequest;
import com.openforms.user.dto.RefreshRequest;
import com.openforms.user.dto.RegisterRequest;
import com.openforms.user.dto.TokenResponse;
import com.openforms.user.repository.RefreshTokenRepository;
import com.openforms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

/**
 * 재사용 탐지의 <b>전량 폐기가 실제로 커밋되는지</b>를 검증합니다.
 *
 * <p>이 클래스만 {@code @Transactional} 을 쓰지 않습니다. 다른 통합 테스트처럼 테스트 트랜잭션으로
 * 감싸면 요청들이 하나의 트랜잭션을 공유해 <b>커밋 여부를 볼 수 없기</b> 때문입니다. 실제로 폐기가
 * 롤백되던 버그(401 을 던지는 순간 같은 트랜잭션의 UPDATE 가 함께 취소됨)가 트랜잭션 테스트에서는
 * 통과했고 런타임 실측에서만 드러났습니다. 그 상황을 여기서 고정합니다.
 *
 * <p>롤백이 없으므로 각 테스트가 남긴 데이터는 {@link #cleanUp()} 이 직접 지웁니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenReuseIntegrationTest {

    private static final String EMAIL = "reuse@example.com";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("재사용이 탐지되면 그 사용자의 다른 토큰도 함께 무효화된다(폐기가 커밋된다)")
    void reuseRevokesEveryTokenOfUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(EMAIL, "password1234", "재사용"))))
                .andExpect(status().isCreated());
        String original = login().refreshToken();
        String rotated = tokensOf(refresh(original).andExpect(status().isOk())).refreshToken();

        // 유출된 사본이 이미 회전된 토큰을 사용 → 탐지
        refresh(original).andExpect(status().isUnauthorized());

        // 탐지의 목적은 정상 클라이언트가 쥐고 있는 토큰까지 끊어 재로그인을 강제하는 것입니다.
        // 폐기가 401 과 함께 롤백되면 이 요청이 200 으로 통과해, 탐지는 로그만 남기고 아무 일도
        // 하지 않은 셈이 됩니다.
        refresh(rotated)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    private TokenResponse login() throws Exception {
        return tokensOf(mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "password1234"))))
                .andExpect(status().isOk()));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))));
    }

    private TokenResponse tokensOf(ResultActions result) throws Exception {
        return objectMapper.readValue(
                result.andReturn().getResponse().getContentAsString(), TokenResponse.class);
    }
}
