package com.openforms.form.dto;

import com.openforms.form.domain.Question;
import com.openforms.form.domain.QuestionType;
import java.util.List;

/**
 * 질문 응답입니다(제작자용 상세). 선택형 타입만 options 를 채우고, RATING/NUMBER 는 min/max 를 씁니다.
 */
public record QuestionResponse(Long id, QuestionType type, String title, boolean required,
        Integer position, Integer minValue, Integer maxValue, List<OptionResponse> options) {

    public static QuestionResponse of(Question question, List<OptionResponse> options) {
        return new QuestionResponse(question.getId(), question.getType(), question.getTitle(),
                question.isRequired(), question.getPosition(), question.getMinValue(),
                question.getMaxValue(), options);
    }
}
