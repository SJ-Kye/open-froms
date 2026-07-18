package com.openforms.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.openforms.common.JpaAuditingConfig;
import com.openforms.form.Form;
import com.openforms.form.FormRepository;
import com.openforms.user.User;
import com.openforms.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 엔티티-스키마 매핑과 JPA Auditing 이 실제로 동작하는지 검증합니다.
 * ddl-auto=validate 이므로 이 테스트가 뜨는 것 자체가 매핑 정합의 증거이고,
 * 아래 단언은 UUIDv7 생성과 감사 컬럼 자동 채움까지 확인합니다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class EntityMappingTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private FormRepository forms;

    @Test
    @DisplayName("User 저장 시 UUIDv7 PK 와 감사 4종이 자동으로 채워진다")
    void userAuditingAndUuid() {
        User saved = users.save(new User("creator@example.com", "hash", "홍길동"));

        assertThat(saved.getId()).isNotNull();
        // UUIDv7 은 version 7 입니다.
        assertThat(saved.getId().version()).isEqualTo(7);
        // 비인증 컨텍스트이므로 생성자는 ANONYMOUS 입니다(회원가입 특성과 일치).
        assertThat(saved.getCreatedBy()).isEqualTo("ANONYMOUS");
        assertThat(saved.getCreatedAt()).isNotNull();
        // 생성 시점에는 updated_* 가 created_* 와 동일하게 채워집니다.
        assertThat(saved.getUpdatedBy()).isEqualTo("ANONYMOUS");
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    @DisplayName("Form 은 UUID FK(user_id)로 User 를 참조하고 감사 컬럼이 채워진다")
    void formPersistsWithUuidForeignKey() {
        User owner = users.save(new User("owner@example.com", "hash", "김소유"));

        Form saved = forms.save(new Form(owner, "고객 만족도 조사", "설명", "a1b2c3d4"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(owner.getId());
        assertThat(saved.getStatus().name()).isEqualTo("DRAFT");
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
