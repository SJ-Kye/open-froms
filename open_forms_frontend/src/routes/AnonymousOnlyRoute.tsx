import { Navigate, Outlet } from 'react-router-dom'
import Spinner from '../components/Spinner'
import { useAuth } from '../features/auth/useAuth'

/**
 * 로그인·회원가입 화면입니다. 이미 로그인한 사용자가 열면 폼 목록으로 돌려보냅니다 — 로그인한
 * 채로 로그인 화면을 보는 것은 언제나 사용자의 착오이고, 거기서 다시 로그인해 봐야 같은 자리로
 * 오기 때문입니다.
 */
export default function AnonymousOnlyRoute() {
  const { status } = useAuth()

  if (status === 'loading') {
    return <Spinner page />
  }
  if (status === 'authed') {
    return <Navigate to="/forms" replace />
  }
  return <Outlet />
}
