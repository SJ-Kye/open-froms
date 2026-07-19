import { createContext } from 'react'
import type { UserResponse } from '../../types/api'
import type { RegisterInput } from './authApi'

/**
 * 인증 상태입니다. `loading` 이 별도 상태인 것이 중요합니다 — 새로고침 직후에는 토큰이 유효한지
 * 아직 모르는데, 이 구간을 `anon` 으로 뭉뚱그리면 보호 라우트가 잠깐 로그인 화면으로 튑니다.
 */
export type AuthStatus = 'loading' | 'authed' | 'anon'

export interface AuthContextValue {
  status: AuthStatus
  user: UserResponse | null
  login: (email: string, password: string) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => Promise<void>
}

/** 컨텍스트를 컴포넌트 파일과 분리해 둡니다(같은 파일이면 Fast Refresh 가 깨집니다). */
export const AuthContext = createContext<AuthContextValue | null>(null)
