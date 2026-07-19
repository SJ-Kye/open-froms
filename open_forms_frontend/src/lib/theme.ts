/**
 * 라이트/다크 테마 전환입니다. `<html>` 의 `dark` 클래스 하나로 tokens.css 의 변수 묶음이 통째로
 * 바뀝니다.
 *
 * 초기값은 **사용자가 고른 값 > OS 설정** 순으로 결정합니다. OS 설정만 따르면 사용자가 바꿀 수
 * 없고, 저장값만 보면 첫 방문에 OS 의 다크 모드를 무시하게 됩니다.
 */

const STORAGE_KEY = 'openforms.theme'

export type Theme = 'light' | 'dark'

function prefersDark(): boolean {
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false
}

/** 저장된 선택이 있으면 그것을, 없으면 OS 설정을 따릅니다. */
export function readTheme(): Theme {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored === 'light' || stored === 'dark') {
    return stored
  }
  return prefersDark() ? 'dark' : 'light'
}

/** 테마를 적용하고 선택을 기억합니다. */
export function applyTheme(theme: Theme): void {
  document.documentElement.classList.toggle('dark', theme === 'dark')
  localStorage.setItem(STORAGE_KEY, theme)
}
