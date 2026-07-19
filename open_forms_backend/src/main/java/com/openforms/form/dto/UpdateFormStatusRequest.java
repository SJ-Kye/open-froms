package com.openforms.form.dto;

import com.openforms.form.domain.FormStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 폼 상태 변경 요청입니다. 허용되지 않는 전이는 서비스에서 409 로 거부됩니다.
 *
 * @param status 전이할 목표 상태입니다(필수).
 */
public record UpdateFormStatusRequest(@NotNull FormStatus status) {
}
