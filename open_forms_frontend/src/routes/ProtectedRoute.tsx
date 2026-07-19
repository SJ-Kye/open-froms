import { Navigate, Outlet, useLocation } from 'react-router-dom'
import Spinner from '../components/Spinner'
import { useAuth } from '../features/auth/useAuth'

/**
 * 제작자 전용 라우트를 감쌉니다.
 *
 * <p>`loading` 동안 로그인 화면으로 보내지 않는 것이 핵심입니다. 새로고침 직후에는 토큰 유효성을
 * 아직 확인하지 못했을 뿐인데, 그 구간을 미인증으로 취급하면 정상 로그인 사용자가 새로고침할
 * 때마다 로그인 화면이 번쩍입니다.
 *
 * <p>가려던 경로는 `state.from` 으로 넘겨, 로그인 후 그 자리로 돌려보냅니다.
 */
export default function ProtectedRoute() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return <Spinner page />
  }
  if (status === 'anon') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}
