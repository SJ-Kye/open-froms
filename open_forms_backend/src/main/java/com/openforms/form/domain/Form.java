package com.openforms.form.domain;

import com.openforms.common.entity.AuditableEntity;
import com.openforms.common.exception.ConflictException;
import com.openforms.user.domain.User;
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

/** 설문 폼입니다. 소유자(user)는 UUID PK 를 갖는 User 이므로 user_id FK 도 UUID 입니다. */
@Entity
@Table(name = "forms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Form extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FormStatus status;

    @Column(name = "slug", nullable = false, unique = true, length = 64)
    private String slug;

    public Form(User user, String title, String description, String slug) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.slug = slug;
        this.status = FormStatus.DRAFT;
    }

    /** 제목·설명을 갱신합니다(감사 updated_by/at 은 JPA Auditing 이 자동 반영). */
    public void updateDetails(String title, String description) {
        this.title = title;
        this.description = description;
    }

    /**
     * 상태를 전이합니다. 허용되지 않는 전이(역방향·건너뛰기·동일 상태)는 {@link ConflictException}(409)로
     * 막아 폼의 생명주기 불변식을 엔티티가 스스로 지킵니다.
     */
    public void changeStatus(FormStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "허용되지 않는 상태 전이입니다: " + this.status + " → " + target);
        }
        this.status = target;
    }
}
