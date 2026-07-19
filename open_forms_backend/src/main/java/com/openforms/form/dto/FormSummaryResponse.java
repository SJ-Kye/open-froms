package com.openforms.form.dto;

import com.openforms.form.domain.Form;
import com.openforms.form.domain.FormStatus;
import java.time.LocalDateTime;

/**
 * 폼 목록 항목 응답입니다. 상세와 달리 질문은 생략하고 응답 수(responseCount)를 함께 노출합니다.
 */
public record FormSummaryResponse(Long id, String title, FormStatus status, long responseCount,
        LocalDateTime createdAt) {

    public static FormSummaryResponse of(Form form, long responseCount) {
        return new FormSummaryResponse(form.getId(), form.getTitle(), form.getStatus(),
                responseCount, form.getCreatedAt());
    }
}
