package com.openforms.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * 액세스 토큰 갱신에 쓰이는 리프레시 토큰입니다. 원문은 보관하지 않고 SHA-256 해시만 남기므로, 이
 * 엔티티만으로는 토큰을 복원할 수 없습니다(해시 계산은 {@code RefreshTokenService} 담당).
 *
 * <p>토큰의 유효 여부는 "폐기되지 않았고 아직 만료 전"이라는 두 조건뿐이며, 그 판정을
 * {@link #isUsable(LocalDateTime)} 로 엔티티가 직접 갖습니다. 사용된 토큰은 삭제하지 않고
 * {@link #revoke(LocalDateTime)} 로 표시만 하여, 나중에 같은 토큰이 다시 오면 유출로 판정할 수 있게
 * 흔적을 남깁니다.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 폐기 시각입니다. 회전·로그아웃·재사용 탐지로 무효화되며, 아직 유효하면 null 입니다. */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public RefreshToken(User user, String tokenHash, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    /** 폐기되지 않았고 아직 만료되지 않았으면 사용할 수 있습니다. */
    public boolean isUsable(LocalDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    /** 이미 폐기된 토큰인지 여부입니다. 재사용 탐지의 판단 근거입니다. */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /** 토큰을 폐기합니다. 이미 폐기된 토큰이면 최초 폐기 시각을 유지합니다(탐지 시점 보존). */
    public void revoke(LocalDateTime at) {
        if (revokedAt == null) {
            this.revokedAt = at;
        }
    }
}
