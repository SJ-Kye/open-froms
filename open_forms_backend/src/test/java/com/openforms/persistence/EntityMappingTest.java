package com.openforms.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.openforms.common.config.JpaAuditingConfig;
import com.openforms.form.domain.Form;
import com.openforms.form.repository.FormRepository;
import com.openforms.user.domain.RefreshToken;
import com.openforms.user.domain.User;
import com.openforms.user.repository.RefreshTokenRepository;
import com.openforms.user.repository.UserRepository;
import java.time.LocalDateTime;
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

    @Autowired
    private RefreshTokenRepository refreshTokens;

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

    @Test
    @DisplayName("RefreshToken 은 해시로 조회되고 폐기 여부로 사용 가능성이 결정된다")
    void refreshTokenPersistsAndRevokes() {
        User owner = users.save(new User("token@example.com", "hash", "박토큰"));
        LocalDateTime now = LocalDateTime.now();

        RefreshToken saved = refreshTokens.save(
                new RefreshToken(owner, "a".repeat(64), now, now.plusDays(14)));

        // 원문이 아니라 해시로 찾습니다(서비스가 제시된 토큰을 해싱해 대조하는 경로).
        assertThat(refreshTokens.findByTokenHash("a".repeat(64))).containsSame(saved);
        assertThat(saved.isUsable(now)).isTrue();

        saved.revoke(now);
        assertThat(saved.isUsable(now)).isFalse();
        assertThat(saved.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰은 폐기되지 않았어도 사용할 수 없다")
    void expiredRefreshTokenIsNotUsable() {
        User owner = users.save(new User("expired@example.com", "hash", "최만료"));
        LocalDateTime now = LocalDateTime.now();

        RefreshToken expired = refreshTokens.save(
                new RefreshToken(owner, "b".repeat(64), now.minusDays(15), now.minusDays(1)));

        assertThat(expired.isRevoked()).isFalse();
        assertThat(expired.isUsable(now)).isFalse();
    }
}
