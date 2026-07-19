import { useContext } from 'react'
import { ToastContext, type ShowToast } from './toastContext'

/** 성공 알림을 띄웁니다. Provider 밖에서 호출하면 즉시 실패시켜 원인을 분명히 합니다. */
export function useToast(): ShowToast {
  const showToast = useContext(ToastContext)
  if (!showToast) {
    throw new Error('useToast 는 ToastProvider 안에서만 사용할 수 있습니다.')
  }
  return showToast
}
