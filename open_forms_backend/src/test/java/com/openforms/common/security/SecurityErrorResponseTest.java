package com.openforms.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 필터 단 401/403 이 공통 에러 포맷(JSON)으로 반환되는지 검증합니다.
 */
class SecurityErrorResponseTest {

    // Boot 4 는 Jackson 3(tools.jackson)을 사용합니다. java.time 은 기본 등록됩니다.
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("미인증 진입점은 401 UNAUTHORIZED JSON 을 쓴다")
    void entryPointWrites401() throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/forms");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("no token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("\"code\":\"UNAUTHORIZED\"");
        assertThat(response.getContentAsString()).contains("\"path\":\"/api/forms\"");
    }

    @Test
    @DisplayName("인가 실패 핸들러는 403 ACCESS_DENIED JSON 을 쓴다")
    void accessDeniedWrites403() throws Exception {
        RestAccessDeniedHandler handler = new RestAccessDeniedHandler(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/forms/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":\"ACCESS_DENIED\"");
    }
}
