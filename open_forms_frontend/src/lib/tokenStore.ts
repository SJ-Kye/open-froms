/**
 * 토큰 보관소입니다. **localStorage 를 만지는 유일한 자리**로, 다른 모듈은 이 함수들만 씁니다.
 * 보관 위치를 바꾸게 되면(예: 리프레시만 httpOnly 쿠키로) 고칠 파일이 여기 하나가 됩니다.
 *
 * <p>localStorage 를 고른 것은 리프레시 토큰이 있어도 액세스 토큰을 메모리에만 두면 새로고침
 * 때마다 토큰 갱신 왕복이 필요하고, 리프레시 토큰까지 메모리에 두면 새로고침이 곧 로그아웃이기
 * 때문입니다. 대가로 XSS 에 노출되며, 그 위험은 회전·재사용 탐지(유출된 토큰이 쓰이면 전량
 * 폐기)로 줄이되 없애지는 못합니다 — `docs/07-limitations` 에 남길 트레이드오프입니다.
 */

const ACCESS_KEY = 'openforms.accessToken'
const REFRESH_KEY = 'openforms.refreshToken'

export interface StoredTokens {
  accessToken: string
  refreshToken: string
}

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY)
}

export function saveTokens(tokens: StoredTokens): void {
  localStorage.setItem(ACCESS_KEY, tokens.accessToken)
  localStorage.setItem(REFRESH_KEY, tokens.refreshToken)
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
}
