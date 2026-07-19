package com.openforms.response.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 질문 하나에 대한 응답입니다. 질문 타입에 맞는 필드 <b>하나만</b> 채웁니다(05 문서의 타입 매핑표).
 *
 * <ul>
 *   <li>SHORT_TEXT·LONG_TEXT → {@code textValue}</li>
 *   <li>SINGLE_CHOICE·DROPDOWN → {@code selectedOptionIds} (1개), MULTIPLE_CHOICE → (N개)</li>
 *   <li>RATING·NUMBER → {@code numberValue}, DATE → {@code dateValue}, TIME → {@code timeValue}</li>
 * </ul>
 *
 * 타입과 무관한 필드가 함께 오면 무시합니다. 값 자체의 적합성은 서버가 질문 타입을 보고 검증합니다.
 */
public record AnswerRequest(
        @NotNull Long questionId,
        List<Long> selectedOptionIds,
        String textValue,
        Integer numberValue,
        LocalDate dateValue,
        LocalTime timeValue) {
}
