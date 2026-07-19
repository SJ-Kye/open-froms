import { createContext } from 'react'

/**
 * 성공 알림을 띄우는 함수입니다.
 *
 * <p>**성공만** 토스트로 보냅니다. 토스트는 몇 초 뒤 사라지므로, 조치가 필요한 정보(실패 사유·
 * 필드별 오류·서버 로그 대조용 traceId)를 담기에 부적합합니다. 실패는 지금까지처럼 `ErrorBanner`
 * 로 문맥 안에 남깁니다.
 */
export type ShowToast = (message: string) => void

/** 컨텍스트를 컴포넌트 파일과 분리해 둡니다(같은 파일이면 Fast Refresh 가 깨집니다). */
export const ToastContext = createContext<ShowToast | null>(null)
