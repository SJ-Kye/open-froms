package com.openforms.common.security;

import com.openforms.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

/**
 * 필터 단계(스프링 시큐리티)에서 발생한 인증/인가 실패를 {@link ErrorResponse} 공통 포맷으로 직접
 * 씁니다. {@code @RestControllerAdvice} 는 디스패처 이후에만 동작하므로, 필터 단 401/403 도 같은
 * 포맷·traceId 로 통일하기 위해 여기서 처리합니다.
 */
final class SecurityErrorResponder {

    private SecurityErrorResponder() {
    }

    static void write(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
            HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ErrorResponse body = ErrorResponse.of(status, code, message, request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
