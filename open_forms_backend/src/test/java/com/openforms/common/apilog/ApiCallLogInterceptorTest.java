package com.openforms.common.apilog;

import static org.assertj.core.api.Assertions.assertThat;

import com.openforms.common.apilog.domain.ApiCallLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * ApiCallLogInterceptor 가 요청/응답에서 이력 메타데이터를 올바르게 뽑아 라이터에 넘기는지 검증합니다.
 * 비동기 타이밍에 의존하지 않도록 라이터를 동기 캡처 스텁으로 대체합니다.
 */
class ApiCallLogInterceptorTest {

    /** write() 를 가로채 저장 없이 엔트리만 캡처하는 스텁입니다(비동기·DB 없음). */
    static class CapturingWriter extends ApiCallLogWriter {
        ApiCallLog captured;

        CapturingWriter() {
            super(null);
        }

        @Override
        public void write(ApiCallLog entry) {
            this.captured = entry;
        }
    }

    @Test
    @DisplayName("method·path·status 를 기록하고, 비인증 요청의 principal 은 null 이다")
    void capturesMetadata() {
        CapturingWriter writer = new CapturingWriter();
        ApiCallLogInterceptor interceptor = new ApiCallLogInterceptor(writer);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/forms");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(writer.captured).isNotNull();
        assertThat(writer.captured.getMethod()).isEqualTo("POST");
        assertThat(writer.captured.getPath()).isEqualTo("/api/forms");
        assertThat(writer.captured.getStatus()).isEqualTo(201);
        assertThat(writer.captured.getPrincipal()).isNull();
        assertThat(writer.captured.getDurationMs()).isGreaterThanOrEqualTo(0L);
        assertThat(writer.captured.getCreatedAt()).isNotNull();
    }
}
