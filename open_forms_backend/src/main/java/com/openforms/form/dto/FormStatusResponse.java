package com.openforms.form.dto;

import com.openforms.form.domain.Form;
import com.openforms.form.domain.FormStatus;
import java.time.LocalDateTime;

/**
 * 폼 상태 변경(PATCH /status) 응답입니다. 변경된 상태와 식별 정보만 반환합니다.
 */
public record FormStatusResponse(Long id, FormStatus status, String slug, LocalDateTime updatedAt) {

    public static FormStatusResponse from(Form form) {
        return new FormStatusResponse(form.getId(), form.getStatus(), form.getSlug(), form.getUpdatedAt());
    }
}
