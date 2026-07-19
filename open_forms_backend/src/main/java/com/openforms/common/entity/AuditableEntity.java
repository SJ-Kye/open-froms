package com.openforms.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * 생성·변경 감사 4종(created_*, updated_*)을 모두 갖는 엔티티의 공통 상위 타입입니다.
 * 독립적인 생명주기와 변경 이력을 갖는 users·forms 가 상속합니다.
 * 생성 시점에는 Spring Data JPA Auditing 이 updated_* 를 created_* 와 동일 값으로 채웁니다.
 */
@Getter
@MappedSuperclass
public abstract class AuditableEntity extends CreatedEntity {

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
