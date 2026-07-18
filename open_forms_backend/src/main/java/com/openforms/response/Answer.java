package com.openforms.response;

import com.openforms.form.Question;
import com.openforms.form.QuestionOption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 제출된 값 1건입니다. 질문당 1행이 아니라 "값 하나"가 한 행입니다 —
 * 체크박스는 고른 선택지 개수만큼 행이 생기고, 그 외 타입은 1행입니다.
 * 한 행에서 option_id 와 네 개의 값 컬럼 중 정확히 하나만 채워집니다.
 */
@Entity
@Table(name = "answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "response_id", nullable = false)
    private Response response;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private QuestionOption option;

    @Column(name = "text_value", columnDefinition = "text")
    private String textValue;

    @Column(name = "number_value")
    private Integer numberValue;

    @Column(name = "date_value")
    private LocalDate dateValue;

    @Column(name = "time_value")
    private LocalTime timeValue;

    private Answer(Response response, Question question, QuestionOption option,
            String textValue, Integer numberValue, LocalDate dateValue, LocalTime timeValue) {
        this.response = response;
        this.question = question;
        this.option = option;
        this.textValue = textValue;
        this.numberValue = numberValue;
        this.dateValue = dateValue;
        this.timeValue = timeValue;
    }

    /** 선택형(SINGLE_CHOICE·DROPDOWN·MULTIPLE_CHOICE) 응답 — 고른 선택지 하나당 한 행. */
    public static Answer ofOption(Response response, Question question, QuestionOption option) {
        return new Answer(response, question, option, null, null, null, null);
    }

    /** 단답·장문(SHORT_TEXT·LONG_TEXT) 응답. */
    public static Answer ofText(Response response, Question question, String textValue) {
        return new Answer(response, question, null, textValue, null, null, null);
    }

    /** 평점·숫자(RATING·NUMBER) 응답. */
    public static Answer ofNumber(Response response, Question question, Integer numberValue) {
        return new Answer(response, question, null, null, numberValue, null, null);
    }

    /** 날짜(DATE) 응답. */
    public static Answer ofDate(Response response, Question question, LocalDate dateValue) {
        return new Answer(response, question, null, null, null, dateValue, null);
    }

    /** 시간(TIME) 응답. */
    public static Answer ofTime(Response response, Question question, LocalTime timeValue) {
        return new Answer(response, question, null, null, null, null, timeValue);
    }
}
