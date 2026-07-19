import { useCallback, useRef, useState, type ReactNode } from 'react'
import { CheckCircle2 } from 'lucide-react'
import { ToastContext } from './toastContext'

interface Toast {
  id: number
  message: string
}

const DURATION_MS = 2600

/**
 * 화면 이동 없이 "방금 한 일이 됐다"를 알립니다. 저장·발행·삭제·복사처럼 **결과가 화면에 곧바로
 * 드러나지 않는** 동작에 씁니다.
 */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  // Date.now() 는 같은 밀리초에 두 번 부르면 키가 겹칩니다. 단조 증가 카운터를 씁니다.
  const nextId = useRef(0)

  const showToast = useCallback<(message: string) => void>((message) => {
    const id = (nextId.current += 1)
    setToasts((current) => [...current, { id, message }])
    window.setTimeout(() => {
      setToasts((current) => current.filter((toast) => toast.id !== id))
    }, DURATION_MS)
  }, [])

  return (
    <ToastContext value={showToast}>
      {children}
      {/*
        aria-live="polite" 라 스크린 리더가 읽던 것을 끊지 않고 다음 차례에 알립니다. 성공 알림은
        지금 하던 일을 중단시킬 만한 정보가 아닙니다.
      */}
      <div className="toast-container" aria-live="polite">
        {toasts.map((toast) => (
          <div key={toast.id} className="toast" role="status">
            <CheckCircle2 size={17} />
            {toast.message}
          </div>
        ))}
      </div>
    </ToastContext>
  )
}
