package com.openforms.user.service;

import com.openforms.user.domain.User;
import java.time.LocalDateTime;

/**
 * 방금 발급한 리프레시 토큰입니다. <b>원문을 담은 유일한 자리</b>이며 영속되지 않습니다(저장은 해시로만
 * 이뤄지므로, 이 값을 클라이언트에 전달하지 못하면 토큰은 영영 복원할 수 없습니다).
 *
 * <p>{@code user} 를 함께 담는 것은 회전 때문입니다. 회전은 "제시된 토큰의 주인에게 새 토큰을 준다"는
 * 동작이라, 호출자가 누구의 토큰인지 다시 조회하지 않고도 액세스 토큰을 발급할 수 있어야 합니다.
 *
 * @param user      토큰의 주인입니다.
 * @param token     클라이언트에 전달할 토큰 원문입니다.
 * @param expiresAt 토큰 만료 시각입니다.
 */
public record IssuedRefreshToken(User user, String token, LocalDateTime expiresAt) {
}
