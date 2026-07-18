package com.openforms.form;

/**
 * 질문 유형 9종입니다. 타입에 따라 응답이 저장되는 위치가 다릅니다(04 DB 설계 참고).
 * <ul>
 *   <li>SHORT_TEXT·LONG_TEXT → answers.text_value</li>
 *   <li>SINGLE_CHOICE·DROPDOWN·MULTIPLE_CHOICE → answers.option_id (체크박스는 N행)</li>
 *   <li>RATING·NUMBER → answers.number_value (min_value~max_value 검증)</li>
 *   <li>DATE → answers.date_value, TIME → answers.time_value</li>
 * </ul>
 * 체크박스는 별도 타입이 아니라 MULTIPLE_CHOICE(다중선택)로 표현합니다.
 */
public enum QuestionType {
    SHORT_TEXT,
    LONG_TEXT,
    SINGLE_CHOICE,
    DROPDOWN,
    MULTIPLE_CHOICE,
    RATING,
    NUMBER,
    DATE,
    TIME
}
