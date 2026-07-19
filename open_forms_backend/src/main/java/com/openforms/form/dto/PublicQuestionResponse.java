package com.openforms.form.dto;

import com.openforms.form.domain.Question;
import com.openforms.form.domain.QuestionType;
import java.util.List;

/**
 * 공개(응답자용) 질문입니다. 응답 화면을 그리는 데 필요한 것만 담습니다 — 입력 위젯을 고르는 {@code type},
 * 필수 표시 {@code required}, 평점·숫자의 허용 범위 {@code minValue}/{@code maxValue}, 선택형의 선택지.
 * 제작자용 {@link QuestionResponse} 와 달리 {@code position} 은 노출하지 않습니다(배열 순서가 곧 순서입니다).
 */
public record PublicQuestionResponse(Long id, QuestionType type, String title, boolean required,
        Integer minValue, Integer maxValue, List<PublicOptionResponse> options) {

    public static PublicQuestionResponse of(Question question, List<PublicOptionResponse> options) {
        return new PublicQuestionResponse(question.getId(), question.getType(), question.getTitle(),
                question.isRequired(), question.getMinValue(), question.getMaxValue(), options);
    }
}
