import { AxiosError } from 'axios'
import { QueryClient } from '@tanstack/react-query'

/**
 * 서버 상태 캐시 설정입니다.
 *
 * <p>기본 재시도(3회)를 그대로 두면 4xx 도 재시도합니다. 404·403·400 은 같은 요청을 반복해도
 * 결과가 달라지지 않으므로 사용자만 기다리게 하고, 401 은 이미 apiClient 가 토큰 갱신으로
 * 처리하고 있어 중복입니다. 그래서 재시도는 서버 오류·네트워크 실패에만 남깁니다.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (failureCount, error) => {
        const status = error instanceof AxiosError ? error.response?.status : undefined
        if (status && status >= 400 && status < 500) {
          return false
        }
        return failureCount < 2
      },
    },
    mutations: {
      // 제출·삭제는 재시도가 곧 중복 실행 위험이라(응답 제출이 두 번 저장될 수 있습니다)
      // 자동 재시도를 두지 않고 사용자가 다시 누르게 합니다.
      retry: false,
    },
  },
})
