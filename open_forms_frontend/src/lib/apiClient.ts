import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { TokenResponse } from '../types/api'
import { clearTokens, getAccessToken, getRefreshToken, saveTokens } from './tokenStore'

/**
 * 모든 API 호출이 지나가는 axios 인스턴스입니다. 여기서 두 가지를 자동으로 처리하므로 화면 코드는
 * 토큰의 존재를 몰라도 됩니다 — **액세스 토큰 첨부**와 **만료 시 자동 갱신**입니다.
 *
 * <p>기본 baseURL 은 `/api` 이며, 개발 서버가 vite.config.ts 의 프록시로 백엔드에 넘깁니다.
 */

/** 요청당 1회만 재시도하기 위한 표식입니다. */
interface RetriableConfig extends InternalAxiosRequestConfig {
  _retried?: boolean
}

/**
 * 401 이어도 갱신을 시도하지 않는 경로입니다. 로그인·가입 실패의 401 은 자격 문제이지 만료가
 * 아니고, `/refresh` 자체의 401 로 다시 갱신하면 무한 루프가 됩니다.
 */
const NO_REFRESH_PATHS = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout']

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  headers: { 'Content-Type': 'application/json' },
})

/** 갱신까지 실패해 세션이 끝났음을 알리는 구독자들입니다(AuthContext 가 구독). */
const forcedLogoutListeners = new Set<() => void>()

export function onForcedLogout(listener: () => void): () => void {
  forcedLogoutListeners.add(listener)
  return () => forcedLogoutListeners.delete(listener)
}

function endSession(): void {
  clearTokens()
  forcedLogoutListeners.forEach((listener) => listener())
}

/**
 * 진행 중인 갱신 요청입니다. 여러 요청이 동시에 401 을 받아도 갱신은 한 번만 나가야 합니다 —
 * 리프레시 토큰은 1회용이라 동시에 두 번 보내면 두 번째가 "이미 회전된 토큰"으로 취급되어
 * 서버의 재사용 탐지가 오작동하고, 정상 사용자의 세션이 통째로 끊깁니다.
 */
let refreshInFlight: Promise<string> | null = null

function refreshAccessToken(): Promise<string> {
  if (refreshInFlight) {
    return refreshInFlight
  }
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return Promise.reject(new Error('리프레시 토큰이 없습니다.'))
  }

  refreshInFlight = axios
    // 인터셉터가 걸린 apiClient 를 쓰면 갱신 요청의 실패가 다시 갱신을 부릅니다. 별도 호출로 끊습니다.
    .post<TokenResponse>(`${apiClient.defaults.baseURL}/auth/refresh`, { refreshToken })
    .then(({ data }) => {
      saveTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken })
      return data.accessToken
    })
    .finally(() => {
      refreshInFlight = null
    })

  return refreshInFlight
}

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetriableConfig | undefined
    const isExpiredAccessToken =
      error.response?.status === 401 &&
      config &&
      !config._retried &&
      !NO_REFRESH_PATHS.some((path) => config.url?.startsWith(path))

    if (!isExpiredAccessToken) {
      return Promise.reject(error)
    }

    try {
      const accessToken = await refreshAccessToken()
      config._retried = true
      config.headers.Authorization = `Bearer ${accessToken}`
      return apiClient(config)
    } catch {
      // 갱신까지 실패했다면 리프레시 토큰도 만료·폐기된 것입니다. 여기서 세션을 끝내지 않으면
      // 남은 토큰으로 모든 요청이 401 을 반복합니다.
      endSession()
      return Promise.reject(error)
    }
  },
)
