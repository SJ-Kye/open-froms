import { useEffect, useState } from 'react'
import { Moon, Sun } from 'lucide-react'
import { applyTheme, readTheme, type Theme } from '../lib/theme'

/** 라이트/다크를 전환하는 버튼입니다. 선택은 localStorage 에 남아 새로고침해도 유지됩니다. */
export default function ThemeToggle() {
  const [theme, setTheme] = useState<Theme>(readTheme)

  useEffect(() => {
    applyTheme(theme)
  }, [theme])

  const next: Theme = theme === 'dark' ? 'light' : 'dark'

  return (
    <button
      type="button"
      className="btn-icon"
      onClick={() => setTheme(next)}
      aria-label={next === 'dark' ? '다크 모드로 전환' : '라이트 모드로 전환'}
      title={next === 'dark' ? '다크 모드' : '라이트 모드'}
    >
      {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
    </button>
  )
}
