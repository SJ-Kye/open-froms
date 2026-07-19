import { Navigate, Route, Routes } from 'react-router-dom'
import AppShell from '../components/AppShell'
import LoginPage from '../features/auth/LoginPage'
import RegisterPage from '../features/auth/RegisterPage'
import FormBuilderPage from '../features/forms/FormBuilderPage'
import FormTabsLayout from '../features/forms/FormTabsLayout'
import FormsListPage from '../features/forms/FormsListPage'
import PublicFormPage from '../features/public/PublicFormPage'
import NotFoundPage from '../pages/NotFoundPage'
import AnonymousOnlyRoute from './AnonymousOnlyRoute'
import ProtectedRoute from './ProtectedRoute'

/**
 * 라우팅 골격입니다. 경로는 **공개 / 익명 전용 / 제작자 전용** 세 갈래이며, 이 구분이 백엔드의
 * 인증 규약(`/api/public/**` 익명 · 제작자 API 인증)과 그대로 맞물립니다.
 *
 * <p>응답 페이지가 `/f/:slug` 로 짧은 것은 이 주소가 제작자가 사람들에게 복사해 보내는 링크이기
 * 때문입니다. 그리고 보호 라우트 <b>밖</b>에 있어야 합니다 — 응답자는 계정이 없습니다.
 *
 * <p>폼 하나를 다루는 세 화면(편집·응답·집계)은 `FormTabsLayout` 아래 중첩합니다. 탭을 컴포넌트
 * 상태가 아니라 <b>경로</b>로 두어야 새로고침·뒤로가기·링크 공유가 그대로 동작하고, 집계 조회가
 * 편집 화면에 들어갈 때마다 따라붙지 않습니다.
 */
export default function AppRouter() {
  return (
    <Routes>
      {/* 공개: 응답자용 */}
      <Route path="/f/:slug" element={<PublicFormPage />} />

      {/* 익명 전용: 로그인한 사용자가 열면 폼 목록으로 */}
      <Route element={<AnonymousOnlyRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      {/* 제작자 전용 */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route path="/forms" element={<FormsListPage />} />
          <Route path="/forms/:id" element={<FormTabsLayout />}>
            <Route index element={<FormBuilderPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="/" element={<Navigate to="/forms" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
