import { apiClient } from '../../lib/apiClient'
import { getRefreshToken } from '../../lib/tokenStore'
import type { TokenResponse, UserResponse } from '../../types/api'

/** 인증 API 호출을 한곳에 모읍니다. 경로·본문 모양을 화면이 알 필요는 없습니다. */

export interface RegisterInput {
  email: string
  password: string
  name: string
}

export async function register(input: RegisterInput): Promise<UserResponse> {
  const { data } = await apiClient.post<UserResponse>('/auth/register', input)
  return data
}

export async function login(email: string, password: string): Promise<TokenResponse> {
  const { data } = await apiClient.post<TokenResponse>('/auth/login', { email, password })
  return data
}

export async function fetchMe(): Promise<UserResponse> {
  const { data } = await apiClient.get<UserResponse>('/auth/me')
  return data
}

/**
 * 서버에 리프레시 토큰 폐기를 요청합니다.
 *
 * <p>실패해도 삼킵니다. 로그아웃은 사용자 입장에서 "이 브라우저에서 나간다"이고 그 부분은 토큰을
 * 지우는 것으로 이미 달성되므로, 서버 호출 실패 때문에 로그아웃이 막히면 안 됩니다.
 */
export async function logout(): Promise<void> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return
  }
  try {
    await apiClient.post('/auth/logout', { refreshToken })
  } catch {
    // 무시 — 폐기되지 않은 리프레시 토큰은 만료(14일)로 정리됩니다.
  }
}
