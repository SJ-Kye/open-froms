package com.openforms.form.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 폼 생성 요청입니다. 생성 시 상태는 DRAFT, slug 는 서버가 생성합니다.
 *
 * @param title       폼 제목입니다(필수).
 * @param description 폼 설명입니다(선택).
 */
public record CreateFormRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 1000) String description) {
}
