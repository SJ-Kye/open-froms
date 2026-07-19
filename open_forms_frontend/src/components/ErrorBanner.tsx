import { AlertCircle } from 'lucide-react'

/**
 * 실패 사유를 사용자에게 보여 줍니다.
 *
 * `traceId` 는 서버가 응답 본문과 `X-Trace-Id` 헤더에 함께 실어 보내는 요청 식별자입니다. 화면에
 * 작게 노출해 두면 사용자가 그 값만 알려 줘도 서버 로그에서 해당 요청을 바로 찾을 수 있습니다.
 */
export default function ErrorBanner({ message, traceId }: { message: string; traceId?: string }) {
  return (
    <div className="banner banner-error" role="alert">
      <AlertCircle size={18} style={{ flexShrink: 0, marginTop: 1 }} />
      <span>
        {message}
        {traceId && <code className="banner-trace">추적 ID: {traceId}</code>}
      </span>
    </div>
  )
}
