import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Check, Copy, Info, ListPlus, Lock } from 'lucide-react'
import ConfirmDialog from '../../components/ConfirmDialog'
import EmptyState from '../../components/EmptyState'
import ErrorBanner from '../../components/ErrorBanner'
import Spinner from '../../components/Spinner'
import { toApiError, type ApiError } from '../../lib/apiError'
import type { QuestionResponse } from '../../types/api'
import QuestionCard from './QuestionCard'
import QuestionEditor from './QuestionEditor'
import { STATUS_LABELS, nextStatus } from './formStatus'
import { emptyDraft, toDraft, toRequest, type QuestionDraft } from './questionDraft'
import {
  useCreateQuestionMutation,
  useDeleteQuestionMutation,
  useFormQuery,
  useUpdateFormMutation,
  useUpdateQuestionMutation,
} from './useForms'
import styles from './FormBuilderPage.module.css'

/** 편집 중인 질문의 대상입니다. `new` 는 추가, 숫자는 그 id 의 질문 수정입니다. */
type EditTarget = { mode: 'new' } | { mode: 'edit'; question: QuestionResponse } | null

export default function FormBuilderPage() {
  const { id } = useParams()
  const formId = Number(id)

  const { data: form, isPending, isError, error } = useFormQuery(formId)
  const updateForm = useUpdateFormMutation(formId)
  const createQuestion = useCreateQuestionMutation(formId)
  const updateQuestion = useUpdateQuestionMutation(formId)
  const deleteQuestion = useDeleteQuestionMutation(formId)

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [editTarget, setEditTarget] = useState<EditTarget>(null)
  const [draft, setDraft] = useState<QuestionDraft>(emptyDraft)
  const [questionError, setQuestionError] = useState<ApiError | null>(null)
  const [pendingDelete, setPendingDelete] = useState<QuestionResponse | null>(null)
  const [copied, setCopied] = useState(false)

  // 서버에서 받은 값으로 입력칸을 채웁니다. 폼이 바뀔 때만 덮어써야, 사용자가 타이핑하는 도중
  // 백그라운드 리페치가 입력을 되돌리는 일이 생기지 않습니다.
  useEffect(() => {
    if (form) {
      setTitle(form.title)
      setDescription(form.description ?? '')
    }
  }, [form?.id]) // eslint-disable-line react-hooks/exhaustive-deps

  if (isPending) {
    return <Spinner page />
  }
  if (isError) {
    const apiError = toApiError(error)
    return (
      <ErrorBanner message={apiError.message} traceId={apiError.traceId} />
    )
  }

  // 위 가드로 로드가 끝났음이 확정된 값입니다. 아래 콜백들은 렌더 이후에 실행되어 좁혀진 타입이
  // 유지되지 않으므로, 여기서 한 번 붙잡아 둡니다.
  const loaded = form

  // 질문 편집은 DRAFT 에서만 가능합니다(서버 409 FORM_NOT_EDITABLE). 제목·설명은 발행 후에도
  // 고칠 수 있습니다 — 수집된 응답의 의미를 바꾸지 않기 때문입니다.
  const questionsEditable = form.status === 'DRAFT'
  const detailsDirty = title !== form.title && title.trim() !== ''
  const descriptionDirty = description !== (form.description ?? '')
  const dirty = detailsDirty || descriptionDirty
  const target = nextStatus(form.status)
  const publicUrl = `${window.location.origin}/f/${form.slug}`

  function startAdd() {
    setDraft(emptyDraft())
    setQuestionError(null)
    setEditTarget({ mode: 'new' })
  }

  function startEdit(question: QuestionResponse) {
    setDraft(toDraft(question))
    setQuestionError(null)
    setEditTarget({ mode: 'edit', question })
  }

  async function saveDetails() {
    try {
      await updateForm.mutateAsync({ title: title.trim(), description })
    } catch {
      // 오류는 뮤테이션 상태로 배너에 표시합니다.
    }
  }

  async function saveQuestion() {
    if (!editTarget) {
      return
    }
    setQuestionError(null)
    try {
      if (editTarget.mode === 'new') {
        // position 은 1-기반이며 목록 끝에 붙입니다(서버 @Positive).
        await createQuestion.mutateAsync(toRequest(draft, loaded.questions.length + 1))
      } else {
        await updateQuestion.mutateAsync({
          questionId: editTarget.question.id,
          // 수정은 순서를 바꾸지 않으므로 원래 position 을 유지합니다.
          input: toRequest(draft, editTarget.question.position),
        })
      }
      setEditTarget(null)
    } catch (caught) {
      // 저장 실패 시 편집기를 닫지 않습니다. 닫으면 사용자가 방금 입력한 내용을 잃습니다.
      setQuestionError(toApiError(caught))
    }
  }

  async function confirmDelete() {
    if (!pendingDelete) {
      return
    }
    await deleteQuestion.mutateAsync(pendingDelete.id)
    setPendingDelete(null)
  }

  async function copyLink() {
    await navigator.clipboard.writeText(publicUrl)
    setCopied(true)
    window.setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div>
      {!questionsEditable && (
        <div className={`banner ${styles.notice}`} style={{ background: 'var(--bg-tertiary)' }}>
          <Lock size={18} style={{ flexShrink: 0 }} />
          <span>
            {STATUS_LABELS[form.status]} 상태에서는 질문을 수정할 수 없습니다. 이미 수집된 응답과
            질문이 어긋나는 것을 막기 위해서입니다. 제목과 설명은 계속 수정할 수 있습니다.
          </span>
        </div>
      )}

      {target === 'PUBLISHED' && form.questions.length === 0 && (
        <div className={`banner ${styles.notice}`} style={{ background: 'var(--bg-tertiary)' }}>
          <Info size={18} style={{ flexShrink: 0 }} />
          <span>질문을 하나 이상 추가해야 발행할 수 있습니다.</span>
        </div>
      )}

      <section className="card">
        <h2 style={{ fontSize: '1.05rem', marginBottom: 16 }}>폼 정보</h2>

        {updateForm.isError && (
          <div style={{ marginBottom: 16 }}>
            <ErrorBanner {...bannerProps(updateForm.error)} />
          </div>
        )}

        <div className="form-group">
          <label className="form-label" htmlFor="form-title">
            제목
          </label>
          <input
            id="form-title"
            type="text"
            className="input-field"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            disabled={updateForm.isPending}
            maxLength={255}
          />
        </div>

        <div className="form-group">
          <label className="form-label" htmlFor="form-description">
            설명 <span className={styles.muted}>(선택)</span>
          </label>
          <textarea
            id="form-description"
            className="input-field"
            rows={3}
            placeholder="응답자에게 보여 줄 안내 문구입니다."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            disabled={updateForm.isPending}
            maxLength={1000}
          />
        </div>

        <button
          type="button"
          className="btn btn-primary"
          onClick={() => void saveDetails()}
          disabled={!dirty || updateForm.isPending}
        >
          {updateForm.isPending && <Spinner size={14} />}
          {dirty ? '변경 사항 저장' : '저장됨'}
        </button>

        {/* 링크는 발행 이후에만 의미가 있습니다. DRAFT 의 slug 로 열면 응답자는 404 를 봅니다. */}
        {form.status !== 'DRAFT' && (
          <div className={styles.shareRow}>
            <code className={styles.shareLink}>{publicUrl}</code>
            <button type="button" className="btn btn-secondary" onClick={() => void copyLink()}>
              {copied ? <Check size={15} /> : <Copy size={15} />}
              {copied ? '복사됨' : '링크 복사'}
            </button>
          </div>
        )}
      </section>

      <div className={styles.sectionHead}>
        <h2 style={{ fontSize: '1.05rem' }}>질문 {form.questions.length}개</h2>
        {questionsEditable && editTarget === null && (
          <button type="button" className="btn btn-secondary" onClick={startAdd}>
            <ListPlus size={16} />
            질문 추가
          </button>
        )}
      </div>

      {deleteQuestion.isError && (
        <div style={{ marginBottom: 16 }}>
          <ErrorBanner {...bannerProps(deleteQuestion.error)} />
        </div>
      )}
      <div className={styles.questions}>
        {form.questions.length === 0 && editTarget === null && (
          <div className="card">
            <EmptyState
              title="아직 질문이 없습니다"
              description={
                questionsEditable
                  ? '질문을 추가하면 응답자에게 보여집니다.'
                  : '질문 없이 발행된 폼입니다.'
              }
              action={
                questionsEditable ? (
                  <button type="button" className="btn btn-primary" onClick={startAdd}>
                    <ListPlus size={16} />첫 질문 추가
                  </button>
                ) : undefined
              }
            />
          </div>
        )}

        {form.questions.map((question, index) =>
          editTarget?.mode === 'edit' && editTarget.question.id === question.id ? (
            <QuestionEditor
              key={question.id}
              draft={draft}
              onChange={setDraft}
              onSave={() => void saveQuestion()}
              onCancel={() => setEditTarget(null)}
              saving={updateQuestion.isPending}
              error={questionError}
            />
          ) : (
            <QuestionCard
              key={question.id}
              question={question}
              index={index}
              editable={questionsEditable && editTarget === null}
              onEdit={() => startEdit(question)}
              onDelete={() => setPendingDelete(question)}
            />
          ),
        )}

        {editTarget?.mode === 'new' && (
          <QuestionEditor
            draft={draft}
            onChange={setDraft}
            onSave={() => void saveQuestion()}
            onCancel={() => setEditTarget(null)}
            saving={createQuestion.isPending}
            error={questionError}
          />
        )}
      </div>

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

function bannerProps(error: unknown) {
  const apiError = toApiError(error)
  return { message: apiError.message, traceId: apiError.traceId }
}
