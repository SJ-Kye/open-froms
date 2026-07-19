package com.openforms.form.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 선택형 질문의 선택지 요청입니다. 선택형(SINGLE_CHOICE·DROPDOWN·MULTIPLE_CHOICE) 질문에만 쓰이며,
 * 그 외 타입에서는 무시됩니다.
 */
public record OptionRequest(
        @NotBlank @Size(max = 255) String label,
        @NotNull @Positive Integer position) {
}
