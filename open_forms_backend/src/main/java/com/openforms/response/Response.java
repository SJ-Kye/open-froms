package com.openforms.response;

import com.openforms.common.CreatedEntity;
import com.openforms.form.Form;
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

/**
 * 익명 응답 제출 1건입니다. 제출 후 불변이므로 생성 감사(created_*)만 갖습니다.
 * created_at 이 곧 제출 시각이며, created_by 는 응답자(익명이면 ANONYMOUS)입니다.
 */
@Entity
@Table(name = "responses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Response extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_id", nullable = false)
    private Form form;

    public Response(Form form) {
        this.form = form;
    }
}
