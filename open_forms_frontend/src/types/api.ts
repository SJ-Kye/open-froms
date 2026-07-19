/**
 * 백엔드 DTO 와 1:1 로 대응하는 타입입니다. 필드명은 서버 record 와 정확히 같아야 하며,
 * 근거는 `docs/05-api-design.md` 의 요청/응답 예시(실행 캡처값)입니다.
 */

/** 검증 실패(400) 시 필드별 사유입니다. */
export interface FieldError {
  field: string
  message: string
}

/**
 * 모든 에러 응답의 공통 포맷입니다(성공 응답은 래퍼 없이 각 API 의 본문 그대로).
 * 서버의 `common/exception/ErrorResponse` 와 대응합니다.
 */
export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  code: string
  message: string
  path: string
  traceId: string
  fieldErrors: FieldError[]
}

/** 로그인·토큰 갱신 응답입니다. `refreshToken` 은 1회용이라 쓸 때마다 새 값으로 회전합니다. */
export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  /** 액세스 토큰 만료까지 남은 초입니다. */
  expiresIn: number
}

/** 사용자 표현입니다(회원가입 결과·`/me` 공용). 비밀번호 해시는 어떤 응답에도 담기지 않습니다. */
export interface UserResponse {
  id: string
  email: string
  name: string
  createdAt: string
}
