import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Check, Copy, Lock, Plus } from 'lucide-react'
import ConfirmDialog from '../../components/ConfirmDialog'
import ErrorBanner from '../../components/ErrorBanner'
import Spinner from '../../components/Spinner'
import { useToast } from '../../components/useToast'
import { toApiError, type ApiError } from '../../lib/apiError'
import type { QuestionResponse, QuestionType } from '../../types/api'
import QuestionCard from './QuestionCard'
import QuickAddPanel from './QuickAddPanel'
import { STATUS_LABELS } from './formStatus'
import { emptyDraft, isDirty, toDraft, toRequest, type QuestionDraft } from './questionDraft'
import {
  useCreateQuestionMutation,
  useDeleteQuestionMutation,
  useFormQuery,
  useReorderQuestionsMutation,
  useUpdateFormMutation,
  useUpdateQuestionMutation,
} from './useForms'
import styles from './FormBuilderPage.module.css'

/**
 * 설문지 편집 화면입니다. 문항은 모드 전환 없이 **바로 편집**되고, 저장은 카드 단위입니다(서버 API 가
 * 문항 단위이므로 — 여러 문항을 한 번에 보내면 중간에 하나가 400 일 때 어디까지 저장됐는지
 * 알 수 없습니다).
 */
export default function FormBuilderPage() {
  const { id } = useParams()
  const formId = Number(id)
  const showToast = useToast()

  const { data: form, isPending, isError, error } = useFormQuery(formId)
  const updateForm = useUpdateFormMutation(formId)
  const createQuestion = useCreateQuestionMutation(formId)
  const updateQuestion = useUpdateQuestionMutation(formId)
  const deleteQuestion = useDeleteQuestionMutation(formId)
  const reorderQuestions = useReorderQuestionsMutation(formId)

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  /** 저장된 문항의 편집 상태입니다(질문 id → 초안). */
  const [drafts, setDrafts] = useState<Record<number, QuestionDraft>>({})
  /** 아직 저장하지 않은 새 문항입니다. 한 번에 하나만 둡니다(addQuestion 주석 참고). */
  const [newDraft, setNewDraft] = useState<QuestionDraft | null>(null)
  const [savingId, setSavingId] = useState<number | 'new' | null>(null)
  const [cardErrors, setCardErrors] = useState<Record<string, ApiError>>({})
  const [pendingDelete, setPendingDelete] = useState<QuestionResponse | null>(null)
  const [copied, setCopied] = useState(false)

  const questions = form?.questions

  // 서버 값으로 설문지 정보를 채웁니다. 설문지가 바뀔 때만 덮어써야 타이핑 도중 리페치가 입력을 되돌리지
  // 않습니다.
  useEffect(() => {
    if (form) {
      setTitle(form.title)
      setDescription(form.description ?? '')
    }
  }, [form?.id]) // eslint-disable-line react-hooks/exhaustive-deps

  // 문항 목록이 바뀌면 초안을 맞추되 **이미 있는 초안은 유지**합니다. 한 카드를 저장하면 목록이
  // 갱신되는데, 그때 다른 카드에서 편집 중이던 내용까지 날아가면 안 됩니다.
  useEffect(() => {
    if (!questions) {
      return
    }
    setDrafts((previous) => {
      const next: Record<number, QuestionDraft> = {}
      for (const question of questions) {
        next[question.id] = previous[question.id] ?? toDraft(question)
      }
      return next
    })
  }, [questions])

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
  const detailsDirty =
    (title !== form.title && title.trim() !== '') || description !== (form.description ?? '')
  const publicUrl = `${window.location.origin}/f/${form.slug}`
  const draftOf = (question: QuestionResponse) => drafts[question.id] ?? toDraft(question)

  /**
   * 새 문항 카드를 띄웁니다. 서버에 즉시 만들지 않는 이유는 제목이 `@NotBlank` 라 빈 질문 POST 가
   * 400 이기 때문입니다.
   *
   * <p>미저장 카드는 하나로 제한합니다. 여러 개를 띄우면 position 배정이 모호해지고, 저장하지 않은
   * 채 화면을 떠나면 만든 줄 알았던 문항이 조용히 사라집니다. 이미 카드가 떠 있으면 유형만 바꿉니다.
   */
  function addQuestion(type: QuestionType) {
    setNewDraft((current) => (current ? { ...current, type } : emptyDraft(type)))
    setCardErrors(({ new: _dropped, ...rest }) => rest)
  }

  async function saveDetails() {
    try {
      await updateForm.mutateAsync({ title: title.trim(), description })
      showToast('설문지 정보를 저장했습니다.')
    } catch {
      // 실패 사유는 배너로 남깁니다(토스트는 사라지므로 조치가 필요한 정보에 부적합).
    }
  }

  async function saveQuestion(question: QuestionResponse | null, draft: QuestionDraft) {
    const key = question ? String(question.id) : 'new'
    setSavingId(question ? question.id : 'new')
    setCardErrors(({ [key]: _dropped, ...rest }) => rest)
    try {
      if (question) {
        // 수정은 순서를 바꾸지 않으므로 원래 position 을 유지합니다.
        const saved = await updateQuestion.mutateAsync({
          questionId: question.id,
          input: toRequest(draft, question.position),
        })
        // 서버가 다듬은 값(공백 제거·빈 선택지 제외)으로 되돌려 놓아야 카드가 계속 «수정됨» 으로
        // 남지 않습니다.
        setDrafts((previous) => ({ ...previous, [saved.id]: toDraft(saved) }))
        showToast('문항을 저장했습니다.')
      } else {
        await createQuestion.mutateAsync(toRequest(draft, loaded.questions.length + 1))
        setNewDraft(null)
        showToast('문항을 추가했습니다.')
      }
    } catch (caught) {
      // 저장에 실패해도 입력을 지우지 않습니다. 방금 쓴 내용을 잃으면 다시 써야 합니다.
      setCardErrors((previous) => ({ ...previous, [key]: toApiError(caught) }))
    } finally {
      setSavingId(null)
    }
  }

  /**
   * 인접한 두 문항의 position 을 맞바꿉니다.
   *
   * <p>`questions` 에 (form_id, position) 유니크 제약이 없어(V1__init.sql) 교체 도중 값이 잠시 겹쳐도
   * 무방합니다. 조회가 position 오름차순이라 두 번의 PUT 이 끝나면 순서가 그대로 반영됩니다.
   */
  async function moveQuestion(index: number, direction: -1 | 1) {
    const target = index + direction
    if (target < 0 || target >= loaded.questions.length) {
      return
    }
    const moving = loaded.questions[index]
    const swapped = loaded.questions[target]
    try {
      await reorderQuestions.mutateAsync({
        first: { id: moving.id, input: toRequest(draftOf(moving), swapped.position) },
        second: { id: swapped.id, input: toRequest(draftOf(swapped), moving.position) },
      })
    } catch (caught) {
      setCardErrors((previous) => ({ ...previous, [String(moving.id)]: toApiError(caught) }))
    }
  }

  async function confirmDelete() {
    if (!pendingDelete) {
      return
    }
    await deleteQuestion.mutateAsync(pendingDelete.id)
    setPendingDelete(null)
    showToast('문항을 삭제했습니다.')
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
          <div className={styles.infoHead}>
            <input
              type="text"
              className={styles.titleInput}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              disabled={updateForm.isPending}
              maxLength={255}
              placeholder="제목 없는 설문지"
              aria-label="설문지 제목"
            />
            {/* 저장 버튼은 입력을 끝낸 시선이 바로 닿도록 카드 우상단에 둡니다. */}
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => void saveDetails()}
              disabled={!detailsDirty || updateForm.isPending}
              style={{ flexShrink: 0 }}
            >
              {updateForm.isPending && <Spinner size={14} />}
              저장
            </button>
          </div>

          <textarea
            className={styles.descriptionInput}
            rows={2}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            disabled={updateForm.isPending}
            maxLength={1000}
            placeholder="응답자에게 보여 줄 설명을 적어 주세요. (선택)"
            aria-label="설문지 설명"
          />

          {updateForm.isError && (
            <div style={{ marginTop: 14 }}>
              <ErrorBanner {...toBanner(updateForm.error)} />
            </div>
          )}

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

        {deleteQuestion.isError && <ErrorBanner {...toBanner(deleteQuestion.error)} />}

        <div className={styles.sectionHead}>
          <h2 className={styles.sectionTitle}>
            질문 <span className={styles.muted}>{form.questions.length}개</span>
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
          {form.questions.map((question, index) => (
            <QuestionCard
              key={question.id}
              question={question}
              draft={draftOf(question)}
              index={index}
              total={form.questions.length}
              editable={questionsEditable}
              saving={savingId === question.id || reorderQuestions.isPending}
              error={cardErrors[String(question.id)] ?? null}
              dirty={isDirty(draftOf(question), question)}
              onDraftChange={(draft) =>
                setDrafts((previous) => ({ ...previous, [question.id]: draft }))
              }
              onSave={() => void saveQuestion(question, draftOf(question))}
              onRevert={() =>
                setDrafts((previous) => ({ ...previous, [question.id]: toDraft(question) }))
              }
              onDelete={() => setPendingDelete(question)}
              onMove={(direction) => void moveQuestion(index, direction)}
            />
          ))}

          {newDraft && (
            <QuestionCard
              question={null}
              draft={newDraft}
              index={form.questions.length}
              total={form.questions.length + 1}
              editable
              saving={savingId === 'new'}
              error={cardErrors.new ?? null}
              dirty
              onDraftChange={setNewDraft}
              onSave={() => void saveQuestion(null, newDraft)}
              onRevert={() => setNewDraft(null)}
              onDelete={() => setNewDraft(null)}
              onMove={() => undefined}
            />
          )}

          {questionsEditable && !newDraft && (
            <button
              type="button"
              className={`btn ${styles.addButton}`}
              onClick={() => addQuestion('SHORT_TEXT')}
            >
              <Plus size={17} />
              질문 추가
            </button>
          )}

          {!questionsEditable && form.questions.length === 0 && (
            <div className="card">
              <p className={styles.muted} style={{ padding: '20px 0', textAlign: 'center' }}>
                질문 없이 발행된 설문지입니다.
              </p>
            </div>
          )}
        </div>
      </div>

      {questionsEditable && (
        <QuickAddPanel
          onAdd={addQuestion}
          activeType={newDraft?.type ?? null}
          disabled={savingId !== null}
        />
      )}

      <ConfirmDialog
        open={pendingDelete !== null}
        title="질문을 삭제할까요?"
        description={pendingDelete ? `"${pendingDelete.title}" 질문이 삭제됩니다.` : ''}
        confirmLabel="삭제"
        danger
        pending={deleteQuestion.isPending}
        onConfirm={() => void confirmDelete()}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  )
}

function toBanner(error: unknown) {
  const apiError = toApiError(error)
  return { message: apiError.message, traceId: apiError.traceId }
}
