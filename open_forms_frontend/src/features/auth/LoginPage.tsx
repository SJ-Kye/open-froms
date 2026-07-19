import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { CheckCircle2, LogIn, Lock, Mail } from 'lucide-react'
import ErrorBanner from '../../components/ErrorBanner'
import Spinner from '../../components/Spinner'
import ThemeToggle from '../../components/ThemeToggle'
import { toApiError, type ApiError } from '../../lib/apiError'
import { useAuth } from './useAuth'
import styles from './AuthForm.module.css'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)

  const state = location.state as { from?: string; registered?: boolean } | null
  // 보호 라우트에서 튕겨 온 경우 그 자리로 돌려보냅니다(ProtectedRoute 가 넘겨 준 값).
  const from = state?.from ?? '/forms'

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await login(email, password)
      navigate(from, { replace: true })
    } catch (caught) {
      setError(toApiError(caught))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="container-narrow animate-fade-in">
      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <ThemeToggle />
      </div>

      <div className="card">
        <div className={styles.header}>
          <h1 className={styles.title}>로그인</h1>
          <p className={styles.subtitle}>Open Forms 로 설문을 만들고 응답을 모아 보세요.</p>
        </div>

        {/* 가입 직후 넘어온 경우, 왜 로그인 화면에 있는지 알려 줍니다. */}
        {state?.registered && !error && (
          <div className={`banner banner-success ${styles.banner}`}>
            <CheckCircle2 size={18} style={{ flexShrink: 0 }} />
            가입이 완료되었습니다. 방금 만든 계정으로 로그인해 주세요.
          </div>
        )}

        {/*
          로그인 실패는 이메일이 없는 경우와 비밀번호가 틀린 경우를 서버가 구분하지 않습니다
          (계정 열거 방지). 그 의도를 유지하려고 화면도 서버 문구를 그대로 보여 주며, 필드별로
          쪼개 표시하지 않습니다.
        */}
        {error && (
          <div className={styles.banner}>
            <ErrorBanner message={error.message} traceId={error.traceId} />
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label className="form-label" htmlFor="email">
              이메일
            </label>
            <div className={styles.inputWithIcon}>
              <Mail size={18} className={styles.inputIcon} />
              <input
                id="email"
                type="email"
                className="input-field"
                autoComplete="email"
                placeholder="email@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={submitting}
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="password">
              비밀번호
            </label>
            <div className={styles.inputWithIcon}>
              <Lock size={18} className={styles.inputIcon} />
              <input
                id="password"
                type="password"
                className="input-field"
                autoComplete="current-password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={submitting}
                required
              />
            </div>
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? (
              <>
                <Spinner size={16} />
                로그인 중…
              </>
            ) : (
              <>
                <LogIn size={18} />
                로그인
              </>
            )}
          </button>
        </form>

        <p className={styles.switch}>
          아직 계정이 없으신가요?
          <Link to="/register" className={styles.switchLink}>
            회원가입
          </Link>
        </p>
      </div>
    </div>
  )
}
