import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Lock, Mail, User, UserPlus } from 'lucide-react'
import ErrorBanner from '../../components/ErrorBanner'
import Spinner from '../../components/Spinner'
import ThemeToggle from '../../components/ThemeToggle'
import { toApiError, type ApiError } from '../../lib/apiError'
import { useAuth } from './useAuth'
import styles from './AuthForm.module.css'

/** 백엔드 RegisterRequest 의 @Size(min = 8) 와 같은 값입니다. */
const MIN_PASSWORD_LENGTH = 8

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)

  // 비밀번호 확인은 서버에 보낼 값이 아니라 오타 방지 장치이므로 화면에서만 검사합니다.
  const mismatch = passwordConfirm.length > 0 && password !== passwordConfirm

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (mismatch) {
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await register({ email, password, name })
      // 서버는 가입 응답에 토큰을 담지 않습니다(UserResponse 만). 자동 로그인 대신 로그인
      // 화면으로 보내, 방금 만든 자격으로 한 번 들어오게 합니다.
      navigate('/login', { replace: true, state: { registered: true } })
    } catch (caught) {
      setError(toApiError(caught))
    } finally {
      setSubmitting(false)
    }
  }

  // 서버가 필드를 지목한 400(VALIDATION_FAILED)이면 해당 입력칸 옆에 붙입니다.
  const fieldError = (field: string) => error?.fieldErrors[field]

  return (
    <div className="container-narrow animate-fade-in">
      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <ThemeToggle />
      </div>

      <div className="card">
        <div className={styles.header}>
          <h1 className={styles.title}>회원가입</h1>
          <p className={styles.subtitle}>계정을 만들면 폼을 만들고 응답을 모을 수 있습니다.</p>
        </div>

        {/* 필드로 특정되지 않는 실패(예: 409 이메일 중복)만 배너로 보여 줍니다. */}
        {error && Object.keys(error.fieldErrors).length === 0 && (
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
                className={`input-field ${fieldError('email') ? 'has-error' : ''}`}
                autoComplete="email"
                placeholder="email@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={submitting}
                required
              />
            </div>
            {fieldError('email') && <span className="field-error">{fieldError('email')}</span>}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="name">
              이름
            </label>
            <div className={styles.inputWithIcon}>
              <User size={18} className={styles.inputIcon} />
              <input
                id="name"
                type="text"
                className={`input-field ${fieldError('name') ? 'has-error' : ''}`}
                autoComplete="name"
                placeholder="홍길동"
                value={name}
                onChange={(e) => setName(e.target.value)}
                disabled={submitting}
                required
              />
            </div>
            {fieldError('name') && <span className="field-error">{fieldError('name')}</span>}
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
                className={`input-field ${fieldError('password') ? 'has-error' : ''}`}
                autoComplete="new-password"
                placeholder={`${MIN_PASSWORD_LENGTH}자 이상`}
                minLength={MIN_PASSWORD_LENGTH}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={submitting}
                required
              />
            </div>
            {fieldError('password') && <span className="field-error">{fieldError('password')}</span>}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="passwordConfirm">
              비밀번호 확인
            </label>
            <div className={styles.inputWithIcon}>
              <Lock size={18} className={styles.inputIcon} />
              <input
                id="passwordConfirm"
                type="password"
                className={`input-field ${mismatch ? 'has-error' : ''}`}
                autoComplete="new-password"
                placeholder="한 번 더 입력해 주세요"
                value={passwordConfirm}
                onChange={(e) => setPasswordConfirm(e.target.value)}
                disabled={submitting}
                required
              />
            </div>
            {mismatch && <span className="field-error">비밀번호가 일치하지 않습니다.</span>}
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-block"
            disabled={submitting || mismatch}
          >
            {submitting ? (
              <>
                <Spinner size={16} />
                가입 중…
              </>
            ) : (
              <>
                <UserPlus size={18} />
                회원가입
              </>
            )}
          </button>
        </form>

        <p className={styles.switch}>
          이미 계정이 있으신가요?
          <Link to="/login" className={styles.switchLink}>
            로그인
          </Link>
        </p>
      </div>
    </div>
  )
}
