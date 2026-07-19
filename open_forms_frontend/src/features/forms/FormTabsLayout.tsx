import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, BarChart3, Inbox, Pencil, Send, Square } from 'lucide-react'
import ConfirmDialog from '../../components/ConfirmDialog'
import ErrorBanner from '../../components/ErrorBanner'
import Spinner from '../../components/Spinner'
import { useToast } from '../../components/useToast'
import { toApiError } from '../../lib/apiError'
import type { FormStatus } from '../../types/api'
import StatusBadge from './StatusBadge'
import { nextStatus } from './formStatus'
import { UnsavedChangesContext } from './unsavedChangesContext'
import { useChangeStatusMutation, useFormQuery } from './useForms'
import styles from './FormTabsLayout.module.css'

/**
 * 설문지 하나를 다루는 세 화면(편집·응답·집계)의 공통 골격입니다. 제목·상태·상태 전이 버튼은 어느
 * 탭에서나 같은 자리에 있어야 하므로 여기에 둡니다.
 *
 * <p>탭을 컴포넌트 상태가 아니라 **경로**로 두었습니다. 그래야 새로고침·뒤로가기·링크 공유가
 * 그대로 동작하고, 집계 조회가 편집 화면에 들어갈 때마다 따라붙지 않습니다.
 *
 * <p>제목을 위해 설문지 상세를 다시 조회하지만 자식 화면과 **같은 쿼리 키**라 React Query 가 캐시를
 * 공유합니다 — 요청은 한 번만 나갑니다.
 */
export default function FormTabsLayout() {
  const { id } = useParams()
  const formId = Number(id)
  const { data: form, isPending, isError, error } = useFormQuery(formId)
  const changeStatus = useChangeStatusMutation(formId)
  const showToast = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const [pendingStatus, setPendingStatus] = useState<FormStatus | null>(null)

  /** 자식 화면(빌더)이 보고한 미저장 변경 건수입니다. */
  const [unsavedCount, setUnsavedCount] = useState(0)
  /** 확인을 기다리는 이동입니다. 사용자가 «나가기»를 고르면 그때 실행합니다. */
  const [pendingLeave, setPendingLeave] = useState<(() => void) | null>(null)

  const requestLeave = useCallback(
    (go: () => void) => {
      if (unsavedCount === 0) {
        go()
        return
      }
      // setState 에 함수를 그대로 넣으면 갱신 함수로 해석되므로 한 겹 감쌉니다.
      setPendingLeave(() => go)
    },
    [unsavedCount],
  )

  const unsavedChanges = useMemo(
    () => ({ report: setUnsavedCount, requestLeave }),
    [requestLeave],
  )

  // 새로고침·탭 닫기는 라우터가 볼 수 없는 경로라 브라우저에 맡깁니다. 문구는 지정할 수 없고
  // 브라우저 기본 경고가 뜹니다(사양상 커스텀 메시지는 무시됩니다).
  useEffect(() => {
    if (unsavedCount === 0) {
      return
    }
    const handler = (event: BeforeUnloadEvent) => event.preventDefault()
    window.addEventListener('beforeunload', handler)
    return () => window.removeEventListener('beforeunload', handler)
  }, [unsavedCount])

  const guardedNavigate = (to: string) => (event: React.MouseEvent) => {
    // 이미 그 화면이면 이동이 아니므로 잃을 것이 없습니다. 지금 있는 탭을 눌렀다고 경고하면
    // 확인창이 «아무 일도 아닌 것»에 붙어 신뢰를 잃습니다.
    if (unsavedCount === 0 || location.pathname === to) {
      return
    }
    event.preventDefault()
    requestLeave(() => navigate(to))
  }

  if (isPending) {
    return <Spinner page />
  }
  if (isError) {
    const apiError = toApiError(error)
    return (
      <div>
        <BackLink />
        <ErrorBanner message={apiError.message} traceId={apiError.traceId} />
      </div>
    )
  }

  const target = nextStatus(form.status)
  const blockedByEmptyQuestions = target === 'PUBLISHED' && form.questions.length === 0

  return (
    <UnsavedChangesContext.Provider value={unsavedChanges}>
      <div className="animate-fade-in">
        <BackLink onClick={guardedNavigate('/forms')} />

        <div className={styles.head}>
          <div>
            <StatusBadge status={form.status} />
            <h1 className={styles.title}>{form.title}</h1>
          </div>
          {target && (
            <div className={styles.headAction}>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => setPendingStatus(target)}
                // 질문이 없는 설문지를 발행하면 응답자는 빈 화면을 봅니다. 서버가 막는 규칙은 아니지만
                // 의도한 상황일 리 없으므로 화면에서 멈춰 세웁니다.
                disabled={blockedByEmptyQuestions}
              >
                {target === 'PUBLISHED' ? <Send size={16} /> : <Square size={16} />}
                {target === 'PUBLISHED' ? '발행하기' : '응답 마감'}
              </button>
              {/*
                버튼이 비활성인 이유를 버튼 바로 아래 둡니다. 화면 상단 배너로 올리면 정작 버튼을
                누르려 볼 때는 이유가 시야에 없습니다.
              */}
              {blockedByEmptyQuestions && (
                <span className={styles.actionNote}>질문을 하나 이상 추가하면 발행할 수 있습니다.</span>
              )}
            </div>
          )}
        </div>

        <nav className={styles.tabs}>
          <Tab
            to={`/forms/${formId}`}
            end
            icon={<Pencil size={15} />}
            label="편집"
            onClick={guardedNavigate(`/forms/${formId}`)}
          />
          <Tab
            to={`/forms/${formId}/responses`}
            icon={<Inbox size={15} />}
            label="응답"
            onClick={guardedNavigate(`/forms/${formId}/responses`)}
          />
          <Tab
            to={`/forms/${formId}/dashboard`}
            icon={<BarChart3 size={15} />}
            label="집계"
            onClick={guardedNavigate(`/forms/${formId}/dashboard`)}
          />
        </nav>

        {changeStatus.isError && (
          <div style={{ marginBottom: 16 }}>
            <ErrorBanner {...toBanner(changeStatus.error)} />
          </div>
        )}

        <Outlet />

        <ConfirmDialog
          open={pendingStatus !== null}
          title={pendingStatus === 'PUBLISHED' ? '설문지를 발행할까요?' : '응답을 마감할까요?'}
          description={
            pendingStatus === 'PUBLISHED'
              ? '발행하면 공개 링크로 누구나 응답할 수 있습니다. 되돌려 작성 중으로 만들 수 없고, 발행 이후에는 질문을 수정할 수 없습니다.'
              : '마감하면 더 이상 응답을 받지 않습니다. 되돌릴 수 없으며, 이미 모인 응답과 집계는 그대로 볼 수 있습니다.'
          }
          confirmLabel={pendingStatus === 'PUBLISHED' ? '발행' : '마감'}
          danger={pendingStatus === 'CLOSED'}
          pending={changeStatus.isPending}
          onConfirm={() => {
            if (pendingStatus) {
              const done = pendingStatus === 'PUBLISHED' ? '설문지를 발행했습니다.' : '응답을 마감했습니다.'
              void changeStatus.mutateAsync(pendingStatus).then(() => {
                setPendingStatus(null)
                showToast(done)
              })
            }
          }}
          onCancel={() => setPendingStatus(null)}
        />

        <ConfirmDialog
          open={pendingLeave !== null}
          title="저장하지 않은 변경이 있습니다"
          description={`변경 ${unsavedCount}건이 아직 저장되지 않았습니다. 이 화면을 떠나면 사라집니다.`}
          confirmLabel="저장하지 않고 나가기"
          cancelLabel="머무르기"
          danger
          onConfirm={() => {
            const go = pendingLeave
            // 이동하면 빌더가 언마운트되며 보고를 0 으로 정리합니다.
            setPendingLeave(null)
            go?.()
          }}
          onCancel={() => setPendingLeave(null)}
        />
      </div>
    </UnsavedChangesContext.Provider>
  )
}

function Tab({
  to,
  end,
  icon,
  label,
  onClick,
}: {
  to: string
  end?: boolean
  icon: React.ReactNode
  label: string
  onClick: (event: React.MouseEvent) => void
}) {
  return (
    <NavLink
      to={to}
      end={end}
      onClick={onClick}
      className={({ isActive }) => `${styles.tab} ${isActive ? styles.tabActive : ''}`}
    >
      {icon}
      {label}
    </NavLink>
  )
}

function BackLink({ onClick }: { onClick?: (event: React.MouseEvent) => void }) {
  return (
    <Link to="/forms" className={styles.back} onClick={onClick}>
      <ArrowLeft size={16} />
      설문지 목록
    </Link>
  )
}

function toBanner(error: unknown) {
  const apiError = toApiError(error)
  return { message: apiError.message, traceId: apiError.traceId }
}
