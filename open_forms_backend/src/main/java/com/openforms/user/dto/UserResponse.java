package com.openforms.user.dto;

import com.openforms.user.domain.User;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 표현 응답입니다(회원가입 결과·{@code /me} 공용). 비밀번호 해시는 절대 노출하지 않습니다.
 *
 * @param id        UUIDv7 식별자입니다.
 * @param email     로그인 이메일입니다.
 * @param name      표시 이름입니다.
 * @param createdAt 가입 시각입니다.
 */
public record UserResponse(UUID id, String email, String name, LocalDateTime createdAt) {

    /** 엔티티를 경계 밖으로 노출하지 않도록 DTO 로 변환합니다. */
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getCreatedAt());
    }
}
