package com.openforms.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openforms.common.trace.TraceContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GlobalExceptionHandler 가 각 예외를 공통 에러 포맷으로 변환하는지 검증합니다.
 *
 * <p>advice 자체가 검증 대상이므로 스프링 컨텍스트 없이 {@code standaloneSetup} 으로 테스트 컨트롤러 +
 * advice 만 조립합니다(보안/인터셉터/스캔 등 무관한 인프라를 배제 → 슬라이스 의존성 문제 회피).
 */
class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("본문 검증 실패는 400 VALIDATION_FAILED 와 fieldErrors 로 변환된다")
    void validationFailure() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    @DisplayName("잘못된 JSON 본문은 400 MALFORMED_REQUEST 로 변환된다")
    void malformedBody() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("ResourceNotFoundException 은 404 로, 전달한 code 를 유지한다")
    void notFound() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("SAMPLE_NOT_FOUND"))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("ConflictException 은 409 로 변환된다")
    void conflict() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SAMPLE_CONFLICT"));
    }

    @Test
    @DisplayName("서비스단 AccessDeniedException 은 403 ACCESS_DENIED 로 변환된다")
    void accessDenied() throws Exception {
        mockMvc.perform(get("/test/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("미처리 예외는 500 INTERNAL_ERROR 로, 내부 메시지를 노출하지 않는다")
    void unexpected() throws Exception {
        mockMvc.perform(get("/test/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
    }

    @Test
    @DisplayName("매핑된 핸들러가 없는 경로는 404 RESOURCE_NOT_FOUND 로 변환된다")
    void noResource() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/api/typo", "/api/typo");

        ResponseEntity<ErrorResponse> response = handler.handleNoResource(
                ex, new MockHttpServletRequest("GET", "/api/typo"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("에러 본문의 traceId 는 현재 요청의 MDC 값(TraceContext)을 담는다")
    void carriesTraceId() throws Exception {
        MDC.put(TraceContext.TRACE_ID_KEY, "fixed-trace-id");

        mockMvc.perform(get("/test/conflict"))
                .andExpect(jsonPath("$.traceId").value("fixed-trace-id"));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        record SampleRequest(@NotBlank String name) {
        }

        @PostMapping("/validate")
        String validate(@Valid @RequestBody SampleRequest request) {
            return request.name();
        }

        @GetMapping("/not-found")
        String notFound() {
            throw new ResourceNotFoundException("SAMPLE_NOT_FOUND", "대상을 찾을 수 없습니다.");
        }

        @GetMapping("/conflict")
        String conflict() {
            throw new ConflictException("SAMPLE_CONFLICT", "이미 존재합니다.");
        }

        @GetMapping("/denied")
        String denied() {
            throw new AccessDeniedException("소유자가 아닙니다.");
        }

        @GetMapping("/error")
        String error() {
            throw new IllegalStateException("내부 상세 메시지 — 노출되면 안 됨");
        }
    }
}
