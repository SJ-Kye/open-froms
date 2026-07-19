package com.openforms.form.dto;

import com.openforms.form.domain.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 질문 추가·수정 공용 요청입니다. 수정은 전량 교체(옵션 포함)를 의미하므로 생성과 페이로드가 동일합니다.
 *
 * <p>필드 형식은 여기서 검증하고, 타입에 따라 달라지는 교차 규칙(선택형→선택지 ≥2, RATING/NUMBER→min≤max)은
 * {@code QuestionService} 가 {@link com.openforms.common.exception.BadRequestException} 으로 반송합니다.
 * {@code minValue}·{@code maxValue}·{@code options} 는 타입에 해당하지 않으면 무시됩니다.
 */
public record QuestionRequest(
        @NotNull QuestionType type,
        @NotBlank @Size(max = 500) String title,
        boolean required,
        @NotNull @Positive Integer position,
        Integer minValue,
        Integer maxValue,
        @Valid List<OptionRequest> options) {
}
