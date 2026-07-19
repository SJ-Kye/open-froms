import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { onForcedLogout } from '../../lib/apiClient'
import { clearTokens, getAccessToken, saveTokens } from '../../lib/tokenStore'
import type { UserResponse } from '../../types/api'
import * as authApi from './authApi'
import { AuthContext, type AuthContextValue, type AuthStatus } from './authContext'

/** 인증 상태를 보유하고 로그인·가입·로그아웃 동작을 제공합니다. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<UserResponse | null>(null)
  const [status, setStatus] = useState<AuthStatus>(() =>
    // 토큰이 아예 없으면 확인할 것도 없으므로 곧바로 익명입니다(불필요한 /me 왕복 회피).
    getAccessToken() ? 'loading' : 'anon',
  )

  /** 세션 종료를 한 곳에서 처리합니다(토큰·사용자·서버 상태 캐시를 함께 비웁니다). */
  const endSession = useCallback(() => {
    clearTokens()
    setUser(null)
    setStatus('anon')
    // 캐시를 비우지 않으면 다음 로그인 사용자가 이전 사용자의 폼 목록을 잠깐 보게 됩니다.
    queryClient.clear()
  }, [queryClient])

  // 새로고침 복원: 토큰이 있으면 /me 로 주체를 확인합니다. 토큰이 만료되었어도 apiClient 가
  // 리프레시로 되살리므로, 여기서는 성공/실패만 보면 됩니다.
  useEffect(() => {
    if (!getAccessToken()) {
      return
    }
    let cancelled = false
    authApi
      .fetchMe()
      .then((me) => {
        if (!cancelled) {
          setUser(me)
          setStatus('authed')
        }
      })
      .catch(() => {
        if (!cancelled) {
          endSession()
        }
      })
    return () => {
      cancelled = true
    }
  }, [endSession])

  // 갱신까지 실패해 apiClient 가 세션을 끝낸 경우를 따라갑니다. 이 구독이 없으면 토큰은 지워졌는데
  // 화면은 로그인 상태로 남아, 모든 요청이 401 인 채 사용자만 영문을 모릅니다.
  useEffect(() => onForcedLogout(endSession), [endSession])

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      async login(email, password) {
        const tokens = await authApi.login(email, password)
        saveTokens({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken })
        // 토큰의 subject 를 디코딩해 쓰지 않고 /me 로 다시 물어봅니다. 표시 이름 같은 정보는
        // 토큰에 없고, 토큰 페이로드를 신뢰해 화면을 구성하는 습관도 만들지 않기 위해서입니다.
        setUser(await authApi.fetchMe())
        setStatus('authed')
      },
      async register(input) {
        // 가입은 토큰을 발급하지 않습니다(서버가 UserResponse 만 반환). 자동 로그인 대신
        // 로그인 화면으로 보내 사용자가 방금 만든 자격으로 한 번 들어오게 합니다.
        await authApi.register(input)
      },
      async logout() {
        await authApi.logout()
        endSession()
      },
    }),
    [status, user, endSession],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}
