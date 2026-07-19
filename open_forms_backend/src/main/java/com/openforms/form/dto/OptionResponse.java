package com.openforms.form.dto;

import com.openforms.form.domain.QuestionOption;

/**
 * 선택지 응답입니다(제작자용 상세). id·label·position 을 노출합니다.
 */
public record OptionResponse(Long id, String label, Integer position) {

    public static OptionResponse from(QuestionOption option) {
        return new OptionResponse(option.getId(), option.getLabel(), option.getPosition());
    }
}
