import { useEffect, useState, type FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { CheckCircle2, Lock, Send, SearchX } from 'lucide-react'
import EmptyState from '../../components/EmptyState'
import ErrorBanner from '../../components/ErrorBanner'
import Spinner from '../../components/Spinner'
import ThemeToggle from '../../components/ThemeToggle'
import { toApiError, type ApiError } from '../../lib/apiError'
import type { AnswerRequest, PublicFormResponse } from '../../types/api'
import QuestionField from './QuestionField'
import {
  initialDraft,
  missingRequiredIds,
  toAnswerRequests,
  toQuestionErrors,
  type AnswerDraft,
} from './answerDraft'
import { fetchPublicForm, submitResponse } from './publicApi'
import styles from './PublicFormPage.module.css'

/**
 * 익명 응답 화면입니다. 이 경로만 인증 없이 열리며, 제작자용 껍데기(AppShell)를 쓰지 않습니다 —
 * 응답자에게 폼 목록·로그아웃은 의미가 없습니다.
 *
 * <p>화면은 네 갈래입니다: 없는 링크(404) · 마감된 폼 · 응답 입력 · 제출 완료.
 */
export default function PublicFormPage() {
  const { slug = '' } = useParams()
  const [draft, setDraft] = useState<AnswerDraft>({})
  const [questionErrors, setQuestionErrors] = useState<Record<number, string>>({})
  const [formError, setFormError] = useState<ApiError | null>(null)
  const [closedDuringWriting, setClosedDuringWriting] = useState(false)

  const {
    data: form,
    isPending,
    isError,
    error,
  } = useQuery({
    queryKey: ['public-form', slug],
    queryFn: () => fetchPublicForm(slug),
    // 응답 화면은 열려 있는 동안 폼이 바뀌지 않는다고 보고, 창을 오갈 때마다 다시 받지 않습니다.
    // 작성 중이던 입력이 리렌더로 흔들리는 것을 막는 목적도 있습니다.
    refetchOnWindowFocus: false,
    retry: false,
  })

  const submit = useMutation({
    mutationFn: (answers: AnswerRequest[]) => submitResponse(slug, answers),
  })

  useEffect(() => {
    if (form) {
      setDraft(initialDraft(form.questions))
    }
  }, [form?.slug]) // eslint-disable-line react-hooks/exhaustive-deps

  if (isPending) {
    return <Spinner page />
  }

  if (isError) {
    const apiError = toApiError(error)
    return (
      <Layout>
        <div className="card">
          {apiError.status === 404 ? (
            // 서버는 없는 slug 와 미발행 폼을 같은 404 로 답합니다(발행 전 폼의 존재를 감추기
            // 위해). 화면도 그 구분을 만들지 않습니다.
            <EmptyState
              icon={<SearchX size={40} />}
              title="설문을 찾을 수 없습니다"
              description="링크가 잘못되었거나 아직 공개되지 않은 설문입니다."
            />
          ) : (
            <ErrorBanner message={apiError.message} traceId={apiError.traceId} />
          )}
        </div>
      </Layout>
    )
  }

  if (submit.isSuccess) {
    return (
      <Layout>
        <div className={`card ${styles.centered}`}>
          <CheckCircle2 size={48} className={styles.doneIcon} />
          <h1 className={styles.title}>응답이 제출되었습니다</h1>
          {/* 익명 제출이라 응답자가 나중에 열어 볼 수단이 없습니다. 그 사실을 분명히 알립니다. */}
          <p className={styles.description}>
            참여해 주셔서 감사합니다. 익명으로 접수되어 제출한 내용을 다시 확인하거나 수정할 수는
            없습니다.
          </p>
        </div>
      </Layout>
    )
  }

  // 위 가드로 로드가 끝났음이 확정된 값입니다. 아래 핸들러는 렌더 이후에 실행되어 좁혀진 타입이
  // 유지되지 않으므로 여기서 한 번 붙잡아 둡니다.
  const loaded = form
  const closed = form.status === 'CLOSED' || closedDuringWriting

  return (
    <Layout>
      <header className={`card ${styles.header}`}>
        <h1 className={styles.title}>{form.title}</h1>
        {form.description && <p className={styles.description}>{form.description}</p>}
        {!closed && form.questions.some((question) => question.required) && (
          <p className={styles.requiredNote}>
            <span className={styles.star}>*</span> 표시는 필수 문항입니다.
          </p>
        )}
      </header>

      {closed ? (
        <div className="card">
          <EmptyState
            icon={<Lock size={40} />}
            title={closedDuringWriting ? '작성 중에 마감되었습니다' : '마감된 설문입니다'}
            description={
              closedDuringWriting
                ? '제출하는 사이에 설문이 마감되어 응답이 접수되지 않았습니다.'
                : '이 설문은 더 이상 응답을 받지 않습니다.'
            }
          />
        </div>
      ) : (
        <form onSubmit={handleSubmit} noValidate>
          {formError && (
            <div className={styles.banner}>
              <ErrorBanner message={formError.message} traceId={formError.traceId} />
            </div>
          )}

          {form.questions.length === 0 ? (
            <div className="card">
              <EmptyState title="질문이 없는 설문입니다" description="제출할 항목이 없습니다." />
            </div>
          ) : (
            <>
              <div className={styles.questions}>
                {form.questions.map((question, index) => (
                  <QuestionField
                    key={question.id}
                    question={question}
                    index={index}
                    value={draft[question.id] ?? { kind: 'text', value: '' }}
                    onChange={(value) => {
                      setDraft((current) => ({ ...current, [question.id]: value }))
                      // 값을 고치는 순간 그 문항의 오류는 더 이상 사실이 아닙니다.
                      setQuestionErrors(({ [question.id]: _removed, ...rest }) => rest)
                    }}
                    error={questionErrors[question.id]}
                    disabled={submit.isPending}
                  />
                ))}
              </div>

              <div className={styles.submitRow}>
                <button
                  type="submit"
                  className="btn btn-primary btn-block"
                  disabled={submit.isPending}
                >
                  {submit.isPending ? <Spinner size={16} /> : <Send size={18} />}
                  {submit.isPending ? '제출 중…' : '제출하기'}
                </button>
              </div>
            </>
          )}
        </form>
      )}

      <p className={styles.footer}>Open Forms 로 만든 설문입니다.</p>
    </Layout>
  )

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setFormError(null)

    // 필수 누락은 서버도 400 으로 잡지만, 왕복 없이 먼저 알려 줍니다(안내용 — 최종 판정은 서버).
    const missing = missingRequiredIds(loaded.questions, draft)
    if (missing.length > 0) {
      setQuestionErrors(Object.fromEntries(missing.map((id) => [id, '필수 응답입니다.'])))
      focusQuestion(missing[0])
      return
    }

    setQuestionErrors({})
    try {
      await submit.mutateAsync(toAnswerRequests(loaded.questions, draft))
    } catch (caught) {
      handleSubmitFailure(caught, loaded)
    }
  }

  function handleSubmitFailure(caught: unknown, loaded: PublicFormResponse) {
    const apiError = toApiError(caught)

    if (apiError.code === 'FORM_CLOSED') {
      // 조회 시점엔 열려 있었는데 작성하는 사이에 마감된 경우입니다. 값을 고쳐도 제출할 수 없으므로
      // 입력 폼을 걷고 사실을 알립니다.
      setClosedDuringWriting(true)
      return
    }

    // 서버가 문항을 지목한 실패(필수 누락)는 해당 문항에 붙입니다.
    const perQuestion = toQuestionErrors(apiError.fieldErrors)
    if (Object.keys(perQuestion).length > 0) {
      setQuestionErrors(perQuestion)
      const firstId = loaded.questions.find((question) => perQuestion[question.id])?.id
      if (firstId) {
        focusQuestion(firstId)
      }
      return
    }

    // 나머지(UNKNOWN_QUESTION·INVALID_ANSWER_VALUE·ANSWER_OUT_OF_RANGE 등)는 문항을 특정할 수
    // 없으므로 상단 배너로 서버 문구를 그대로 보여 줍니다.
    setFormError(apiError)
  }
}

/** 오류가 난 첫 문항으로 이동시킵니다. 문항이 많으면 화면 밖의 오류를 사용자가 찾지 못합니다. */
function focusQuestion(questionId: number) {
  const element = document.getElementById(`question-${questionId}`)
  element?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  element?.focus({ preventScroll: true })
}

function Layout({ children }: { children: React.ReactNode }) {
  return (
    <div className={`${styles.page} animate-fade-in`}>
      <div className={styles.topBar}>
        <ThemeToggle />
      </div>
      {children}
    </div>
  )
}
