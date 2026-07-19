package com.openforms.form.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 폼에 속한 질문입니다. 폼에 종속되어 함께 생성·삭제되므로 감사 컬럼을 두지 않습니다.
 * min_value/max_value 는 RATING·NUMBER 의 허용 범위이며 그 외 타입에서는 null 입니다.
 */
@Entity
@Table(name = "questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_id", nullable = false)
    private Form form;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private QuestionType type;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "min_value")
    private Integer minValue;

    @Column(name = "max_value")
    private Integer maxValue;

    public Question(Form form, QuestionType type, String title, boolean required, Integer position,
            Integer minValue, Integer maxValue) {
        this.form = form;
        this.type = type;
        this.title = title;
        this.required = required;
        this.position = position;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * 질문 속성을 갱신합니다(수정 시 전량 교체). 선택지는 값이 아니라 별도 엔티티라 서비스가 따로 교체하며,
     * 여기서는 폼 소속은 바꾸지 않습니다.
     */
    public void update(QuestionType type, String title, boolean required, Integer position,
            Integer minValue, Integer maxValue) {
        this.type = type;
        this.title = title;
        this.required = required;
        this.position = position;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }
}
