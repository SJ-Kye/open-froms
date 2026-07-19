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
import java.time.LocalDateTime;
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

    /** 발행 시각입니다. 아직 발행하지 않았으면 null 이며, 일별 응답 추이의 시작점이 됩니다. */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /** 종료 시각입니다. 아직 종료하지 않았으면 null 이며, 일별 응답 추이의 끝점이 됩니다. */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

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
     *
     * <p>전이가 성사된 시각을 함께 못박습니다. 전이는 선형·1회성이므로 각 시각은 한 번만 쓰이며, 이후
     * 제목 수정 등으로 흔들리는 감사 컬럼과 달리 "언제 발행했는가"의 근거로 삼을 수 있습니다.
     */
    public void changeStatus(FormStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "허용되지 않는 상태 전이입니다: " + this.status + " → " + target);
        }
        this.status = target;
        LocalDateTime now = LocalDateTime.now();
        if (target == FormStatus.PUBLISHED) {
            this.publishedAt = now;
        } else if (target == FormStatus.CLOSED) {
            this.closedAt = now;
        }
    }

    /**
     * 질문 편집이 가능한 상태(DRAFT)인지 확인합니다. 발행·종료된 폼의 질문을 바꾸면 이미 수집된 응답과
     * 어긋나므로, DRAFT 가 아니면 {@link ConflictException}(409)로 막습니다.
     */
    public void requireEditable() {
        if (this.status != FormStatus.DRAFT) {
            throw new ConflictException("FORM_NOT_EDITABLE",
                    "발행되었거나 종료된 설문지의 질문은 편집할 수 없습니다.");
        }
    }
}
