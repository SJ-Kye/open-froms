import ThemeToggle from './components/ThemeToggle'

/** 임시 진입 화면입니다. 라우팅·인증이 붙으면서 대체됩니다. */
export default function App() {
  return (
    <div className="main-wrapper">
      <nav className="navbar">
        <span className="nav-brand">Open Forms</span>
        <div className="nav-actions">
          <ThemeToggle />
        </div>
      </nav>
    </div>
  )
}
