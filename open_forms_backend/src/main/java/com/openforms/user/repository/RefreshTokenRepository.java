package com.openforms.user.repository;

import com.openforms.user.domain.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** 제시된 토큰의 해시로 단건 조회합니다(uk_refresh_tokens_hash 가 커버). */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 한 사용자의 유효한 토큰을 모두 폐기합니다. 재사용이 탐지되면 어느 사본이 공격자의 것인지 알 수
     * 없으므로 전부 무효화하고 재로그인을 요구합니다.
     *
     * <p>건별로 엔티티를 불러와 폐기하면 세션 수만큼 UPDATE 가 나가므로 벌크 UPDATE 로 한 번에
     * 처리합니다. 벌크 연산은 영속성 컨텍스트를 우회하므로, 같은 트랜잭션에서 이미 로드한 토큰
     * 엔티티를 이후에 다시 읽지 않도록 호출 위치에 주의합니다.
     *
     * <p><b>{@code REQUIRES_NEW} 인 이유</b>: 호출 지점이 "폐기한 뒤 401 을 던지는" 자리입니다. 호출자의
     * 트랜잭션에 참여하면 그 예외로 폐기까지 함께 롤백되어, 탐지가 아무것도 하지 않은 셈이 됩니다.
     * 별도 트랜잭션으로 즉시 커밋해 호출자가 어떻게 끝나든 폐기가 남도록 합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}
