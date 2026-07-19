package com.openforms.form.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 폼 수정 요청입니다. 제목·설명만 변경하며 상태·slug 는 별도 엔드포인트에서 다룹니다.
 *
 * @param title       폼 제목입니다(필수).
 * @param description 폼 설명입니다(선택).
 */
public record UpdateFormRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 1000) String description) {
}
