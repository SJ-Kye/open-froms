package com.openforms.form.dto;

import com.openforms.form.domain.Form;
import com.openforms.form.domain.FormStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 폼 상세 응답입니다(생성·조회·수정 공용). 질문 목록을 포함합니다.
 */
public record FormDetailResponse(Long id, String title, String description, FormStatus status,
        String slug, List<QuestionResponse> questions, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static FormDetailResponse of(Form form, List<QuestionResponse> questions) {
        return new FormDetailResponse(form.getId(), form.getTitle(), form.getDescription(),
                form.getStatus(), form.getSlug(), questions, form.getCreatedAt(), form.getUpdatedAt());
    }
}
