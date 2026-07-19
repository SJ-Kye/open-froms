package com.openforms.form.dto;

import com.openforms.form.domain.QuestionOption;

/**
 * 공개(응답자용) 선택지입니다. 응답 제출에 필요한 {@code id} 와 화면에 보일 {@code label} 만 노출하며,
 * {@code position} 은 배열 순서가 대신하므로 담지 않습니다.
 */
public record PublicOptionResponse(Long id, String label) {

    public static PublicOptionResponse from(QuestionOption option) {
        return new PublicOptionResponse(option.getId(), option.getLabel());
    }
}
