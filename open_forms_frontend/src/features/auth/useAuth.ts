import { useContext } from 'react'
import { AuthContext, type AuthContextValue } from './authContext'

/** 인증 상태·액션에 접근합니다. Provider 밖에서 호출하면 즉시 실패시켜 원인을 분명히 합니다. */
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth 는 AuthProvider 안에서만 사용할 수 있습니다.')
  }
  return context
}
