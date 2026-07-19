import { Link, Outlet } from 'react-router-dom'
import { FileText, LogOut } from 'lucide-react'
import ThemeToggle from './ThemeToggle'
import { useAuth } from '../features/auth/useAuth'

/** 로그인한 제작자 화면의 공통 골격입니다(상단 바 + 본문). */
export default function AppShell() {
  const { user, logout } = useAuth()

  return (
    <>
      <nav className="navbar">
        <Link to="/forms" className="nav-brand">
          <FileText size={22} />
          Open Forms
        </Link>
        <div className="nav-actions">
          {user && (
            <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>{user.email}</span>
          )}
          <ThemeToggle />
          <button type="button" className="btn btn-secondary" onClick={() => void logout()}>
            <LogOut size={16} />
            로그아웃
          </button>
        </div>
      </nav>
      <main className="main-wrapper">
        <Outlet />
      </main>
    </>
  )
}
