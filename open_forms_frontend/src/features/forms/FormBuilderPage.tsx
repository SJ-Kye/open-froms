import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Check, Copy, Lock, Plus, Save, Undo2 } from 'lucide-react'
import ConfirmDialog from '../../components/ConfirmDialog'
import ErrorBanner from '../../components/ErrorBanner'
import Spinner from '../../components/Spinner'
import { useToast } from '../../components/useToast'
import { toApiError, type ApiError } from '../../lib/apiError'
import type { FormDetailResponse, QuestionType } from '../../types/api'
import QuestionCard from './QuestionCard'
import QuickAddPanel from './QuickAddPanel'
import { STATUS_LABELS } from './formStatus'
import { draftProblem, emptyDraft, newDraftKey } from './questionDraft'
import { buildSavePlan, toItem, type BuilderItem } from './savePlan'
import { useReportUnsavedChanges } from './useUnsavedChanges'
import { useFormQuery, useSaveBuilderMutation, type SaveOutcome } from './useForms'
import styles from './FormBuilderPage.module.css'

/**
 * 설문지 편집 화면입니다. 제목·설명·문항 내용·문항 순서·문항 삭제를 **모두 로컬에서** 고치고,
 * 하단의 저장 한 번으로 서버에 반영합니다.
 *
 * <p>이전에는 설문지 정보와 문항마다 저장 버튼이 따로 있었습니다. 서버 API 가 문항 단위라 그렇게
 * 두었는데, 문항 다섯 개를 고치면 저장을 여섯 번 눌러야 했습니다. 지금은 API 형태를 화면이 그대로
 * 따라가는 대신 <b>보내기 전 전량 검증</b>(draftProblem)으로 부분 실패 위험을 줄이고, 그래도 실패한
 * 항목은 해당 카드에 사유를 붙입니다.
 *
 * <p>순서 변경이 네트워크 없이 되는 이유는 저장 시 <b>전 문항의 position 을 화면 순서대로 새로
 * 부여</b>하기 때문입니다(savePlan 참고).
 */
export default function FormBuilderPage() {
  const { id } = useParams()
  const formId = Number(id)
  const showToast = useToast()

  const { data: form, isPending, isError, error, refetch } = useFormQuery(formId)
  const saveBuilder = useSaveBuilderMutation(formId)

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [items, setItems] = useState<BuilderItem[]>([])
  /** 방금 추가한 카드입니다. 포커스를 여기에만 줍니다. */
  const [focusKey, setFocusKey] = useState<string | null>(null)
  const [cardErrors, setCardErrors] = useState<Record<string, ApiError>>({})
  /** 저장을 한 번이라도 눌렀는지. 검증 사유를 그 전까지는 숨깁니다. */
  const [attempted, setAttempted] = useState(false)
  const [failureNote, setFailureNote] = useState<string | null>(null)
  const [confirmingDeletes, setConfirmingDeletes] = useState(false)
  const [copied, setCopied] = useState(false)

  const plan = form ? buildSavePlan(form, title, description, items) : null
  const changeCount = plan?.changeCount ?? 0

  // 백그라운드 리페치가 편집 중인 초안을 되돌리면 안 되므로, 변경이 있는 동안에는 서버 값으로
  // 덮어쓰지 않습니다. 저장 직후의 동기화는 save() 가 직접 합니다(부분 실패까지 감안해야 하므로).
  const dirtyRef = useRef(false)
  dirtyRef.current = changeCount > 0

  /**
   * 첫 동기화 여부입니다. 이 구분이 없으면 화면이 뜨지 않습니다 — items 가 비어 있는 최초 렌더에서
   * 계획은 «서버 문항 전부 삭제»로 계산되고, 그 결과 dirty 로 판정되어 동기화가 영영 막힙니다.
   */
  const syncedRef = useRef(false)

  useEffect(() => {
    if (form && (!syncedRef.current || !dirtyRef.current)) {
      syncFrom(form)
      syncedRef.current = true
    }
  }, [form])

  useReportUnsavedChanges(changeCount)

  if (isPending) {
    return <Spinner page />
  }
  if (isError) {
    const apiError = toApiError(error)
    return <ErrorBanner message={apiError.message} traceId={apiError.traceId} />
  }

  const loaded = form
  // 질문 편집은 DRAFT 에서만 가능합니다(서버 409 FORM_NOT_EDITABLE). 제목·설명은 발행 후에도
  // 고칠 수 있습니다 — 수집된 응답의 의미를 바꾸지 않기 때문입니다.
  const questionsEditable = form.status === 'DRAFT'
  const publicUrl = `${window.location.origin}/f/${form.slug}`
  const saving = saveBuilder.isPending

  function syncFrom(fresh: FormDetailResponse) {
    setTitle(fresh.title)
    setDescription(fresh.description ?? '')
    setItems(fresh.questions.map(toItem))
  }

  function addQuestion(type: QuestionType) {
    const key = newDraftKey()
    setItems((current) => [...current, { key, questionId: null, draft: emptyDraft(type) }])
    setFocusKey(key)
  }

  function moveQuestion(index: number, direction: -1 | 1) {
    const target = index + direction
    setItems((current) => {
      if (target < 0 || target >= current.length) {
        return current
      }
      const next = [...current]
      ;[next[index], next[target]] = [next[target], next[index]]
      return next
    })
  }

  function revertAll() {
    syncFrom(loaded)
    setCardErrors({})
    setFailureNote(null)
    setAttempted(false)
  }

  /**
   * 보내기 전에 모든 카드를 검사합니다. 하나라도 걸리면 **아무것도 보내지 않고** 첫 문제 카드로
   * 데려갑니다. 일부만 저장돼 어디까지 반영됐는지 모르게 되는 상황을 여기서 대부분 막습니다.
   *
   * @returns 문제가 없으면 true
   */
  function validateAll(): boolean {
    const problems: Record<string, ApiError> = {}
    for (const item of items) {
      const problem = draftProblem(item.draft)
      if (problem) {
        problems[item.key] = { status: 0, code: 'CLIENT_VALIDATION', message: problem, fieldErrors: {} }
      }
    }
    if (title.trim() === '') {
      setFailureNote('설문지 제목을 입력해 주세요.')
      setCardErrors(problems)
      return false
    }
    setCardErrors(problems)
    if (Object.keys(problems).length === 0) {
      return true
    }
    const firstKey = items.find((item) => problems[item.key])?.key
    if (firstKey) {
      focusCard(firstKey)
    }
    setFailureNote('저장할 수 없는 문항이 있습니다. 표시된 곳을 고쳐 주세요.')
    return false
  }

  async function save() {
    setAttempted(true)
    setFailureNote(null)
    if (!plan || plan.changeCount === 0 || !validateAll()) {
      return
    }
    if (plan.deletes.length > 0 && !confirmingDeletes) {
      setConfirmingDeletes(true)
      return
    }
    setConfirmingDeletes(false)

    const snapshot = items
    const outcome = await saveBuilder.mutateAsync(plan)
    // 무효화만으로는 이 자리에서 새 데이터를 손에 쥘 수 없는데, 아래 reconcile 이 서버 값과 실패한
    // 초안을 함께 놓고 판단해야 하므로 여기서 직접 받아 옵니다.
    const { data: fresh } = await refetch()
    if (fresh) {
      reconcile(fresh, snapshot, outcome)
    }

    if (outcome.failures.length === 0) {
      showToast(`변경 ${outcome.succeeded}건을 저장했습니다.`)
      setAttempted(false)
      return
    }
    // 실패는 사라지는 토스트가 아니라 화면에 남는 배너로 알립니다.
    setFailureNote(
      `${outcome.succeeded}건은 저장했지만 ${outcome.failures.length}건이 실패했습니다.`,
    )
  }

  /**
   * 저장 뒤 화면을 서버 상태에 맞춥니다. 다만 **실패한 항목은 사용자가 쓴 값을 되살립니다** —
   * 서버가 거부한 것이지 사용자가 취소한 것이 아니므로, 서버 값으로 덮으면 방금 쓴 내용이 사라집니다.
   */
  function reconcile(fresh: FormDetailResponse, snapshot: BuilderItem[], outcome: SaveOutcome) {
    const failed = new Set(outcome.failures.map((failure) => failure.key))
    const next: BuilderItem[] = fresh.questions.map((question) => {
      const local = snapshot.find((item) => item.questionId === question.id)
      const item = toItem(question)
      if (local) {
        // 키를 유지해야 카드에 붙은 오류 표시가 다른 카드로 옮겨 가지 않습니다.
        item.key = local.key
        if (failed.has(local.key)) {
          item.draft = local.draft
        }
      }
      return item
    })
    for (const item of snapshot) {
      if (item.questionId === null && failed.has(item.key)) {
        next.push(item)
      }
    }
    setItems(next)
    if (!failed.has('details')) {
      setTitle(fresh.title)
      setDescription(fresh.description ?? '')
    }

    const errors: Record<string, ApiError> = {}
    for (const failure of outcome.failures) {
      errors[failure.key] = toApiError(failure.error)
    }
    setCardErrors(errors)
  }

  function focusCard(key: string) {
    const card = document.querySelector(`[data-question-card="${key}"]`)
    card?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    card?.querySelector('input')?.focus({ preventScroll: true })
  }

  async function copyLink() {
    await navigator.clipboard.writeText(publicUrl)
    setCopied(true)
    showToast('공개 링크를 복사했습니다.')
    window.setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className={styles.layout}>
      <div className={styles.main}>
        <section className={`card ${styles.infoCard}`}>
          <input
            type="text"
            className={styles.titleInput}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            disabled={saving}
            maxLength={255}
            placeholder="제목 없는 설문지"
            aria-label="설문지 제목"
          />

          <textarea
            className={styles.descriptionInput}
            rows={2}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            disabled={saving}
            maxLength={1000}
            placeholder="응답자에게 보여 줄 설명을 적어 주세요. (선택)"
            aria-label="설문지 설명"
          />

          {/* 링크는 발행 이후에만 의미가 있습니다. DRAFT 의 slug 로 열면 응답자는 404 를 봅니다. */}
          {form.status !== 'DRAFT' && (
            <div className={styles.shareRow}>
              <code className={styles.shareLink}>{publicUrl}</code>
              <button type="button" className="btn btn-secondary" onClick={() => void copyLink()}>
                {copied ? <Check size={15} /> : <Copy size={15} />}
                링크 복사
              </button>
            </div>
          )}
        </section>

        <div className={styles.sectionHead}>
          <h2 className={styles.sectionTitle}>
            질문 <span className={styles.muted}>{items.length}개</span>
          </h2>
          {/* 편집이 막힌 이유는 그 제약이 적용되는 자리(질문 섹션) 옆에 둡니다. */}
          {!questionsEditable && (
            <span className={styles.lockNote}>
              <Lock size={13} />
              {STATUS_LABELS[form.status]} 상태에서는 수정할 수 없습니다 — 수집된 응답과 어긋나기
              때문입니다.
            </span>
          )}
        </div>

        <div className={styles.questions}>
          {items.map((item, index) => (
            <QuestionCard
              key={item.key}
              cardKey={item.key}
              question={
                item.questionId === null
                  ? null
                  : (loaded.questions.find((q) => q.id === item.questionId) ?? null)
              }
              draft={item.draft}
              index={index}
              total={items.length}
              editable={questionsEditable}
              saving={saving}
              error={cardErrors[item.key] ?? null}
              dirty={
                item.questionId === null ||
                (plan?.updates.some((update) => update.key === item.key) ?? false)
              }
              showProblem={attempted}
              autoFocus={focusKey === item.key}
              onDraftChange={(draft) =>
                setItems((current) =>
                  current.map((entry) => (entry.key === item.key ? { ...entry, draft } : entry)),
                )
              }
              onDelete={() =>
                setItems((current) => current.filter((entry) => entry.key !== item.key))
              }
              onMove={(direction) => moveQuestion(index, direction)}
            />
          ))}

          {questionsEditable && (
            <button
              type="button"
              className={`btn ${styles.addButton}`}
              onClick={() => addQuestion('SHORT_TEXT')}
              disabled={saving}
            >
              <Plus size={17} />
              질문 추가
            </button>
          )}

          {!questionsEditable && items.length === 0 && (
            <div className="card">
              <p className={styles.muted} style={{ padding: '20px 0', textAlign: 'center' }}>
                질문 없이 발행된 설문지입니다.
              </p>
            </div>
          )}
        </div>

        {/*
          저장 바는 변경이 있을 때만 나타나 화면 아래에 붙어 있습니다. 문항이 많아 스크롤이 길어져도
          «저장하지 않은 것이 있다»는 사실과 저장 수단이 같은 자리에 계속 보여야 합니다.
        */}
        {changeCount > 0 && (
          <div className={styles.saveBar}>
            <div className={styles.saveBarInfo}>
              <span className={styles.saveBarCount}>저장하지 않은 변경 {changeCount}건</span>
              {failureNote && <span className={styles.saveBarError}>{failureNote}</span>}
            </div>
            <div className={styles.saveBarActions}>
              <button type="button" className="btn btn-secondary" onClick={revertAll} disabled={saving}>
                <Undo2 size={15} />
                되돌리기
              </button>
              <button type="button" className="btn btn-primary" onClick={() => void save()} disabled={saving}>
                {saving ? <Spinner size={14} /> : <Save size={15} />}
                저장
              </button>
            </div>
          </div>
        )}
      </div>

      {questionsEditable && <QuickAddPanel onAdd={addQuestion} disabled={saving} />}

      <ConfirmDialog
        open={confirmingDeletes}
        title="삭제한 문항이 있습니다"
        description={`저장하면 문항 ${plan?.deletes.length ?? 0}개가 삭제되며, 되돌릴 수 없습니다.`}
        confirmLabel="저장"
        danger
        pending={saving}
        onConfirm={() => void save()}
        onCancel={() => setConfirmingDeletes(false)}
      />
    </div>
  )
}
