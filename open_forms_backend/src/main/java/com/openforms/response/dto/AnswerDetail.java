package com.openforms.response.dto;

import com.openforms.form.domain.QuestionType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 응답 상세의 문항별 답변 한 건입니다. 저장은 "값 하나가 한 행"이지만(04 문서), 화면은 문항 단위로
 * 읽으므로 여기서 질문 기준으로 다시 묶어 줍니다 — 체크박스의 여러 행이 하나의 {@code selectedOptions} 가 됩니다.
 *
 * <p>{@code answered} 는 이 문항에 답했는지를 값 유무와 별개로 명시합니다. 화면이 타입마다 어느 필드를
 * 봐야 하는지 따지지 않고 무응답을 판정할 수 있습니다.
 */
public record AnswerDetail(Long questionId, QuestionType type, String title, boolean required,
        boolean answered, List<SelectedOption> selectedOptions, String textValue,
        Integer numberValue, LocalDate dateValue, LocalTime timeValue) {

    /** 선택형 답변이 고른 선택지입니다. */
    public record SelectedOption(Long optionId, String label) {
    }
}
