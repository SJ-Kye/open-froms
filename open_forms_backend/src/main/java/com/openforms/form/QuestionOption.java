package com.openforms.form;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** 선택형 질문(SINGLE_CHOICE·DROPDOWN·MULTIPLE_CHOICE)의 선택지입니다. */
@Entity
@Table(name = "question_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "position", nullable = false)
    private Integer position;

    public QuestionOption(Question question, String label, Integer position) {
        this.question = question;
        this.label = label;
        this.position = position;
    }
}
