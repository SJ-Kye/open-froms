package com.openforms.response.dto;

import com.openforms.response.domain.Response;
import java.time.LocalDateTime;

/**
 * 응답 제출 결과입니다. 익명 제출이라 응답 내용을 되돌려주지 않고 접수 사실만 확인해 줍니다.
 * {@code submittedAt} 은 별도 컬럼이 아니라 생성 감사값({@code responses.created_at})입니다.
 */
public record SubmitResponseResult(Long responseId, LocalDateTime submittedAt) {

    public static SubmitResponseResult from(Response response) {
        return new SubmitResponseResult(response.getId(), response.getCreatedAt());
    }
}
