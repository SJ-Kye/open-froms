import { ChevronDown, ChevronUp, Plus, Trash2, X } from 'lucide-react'
import ErrorBanner from '../../components/ErrorBanner'
import Spinner from '../../components/Spinner'
import type { ApiError } from '../../lib/apiError'
import type { QuestionResponse, QuestionType } from '../../types/api'
import { addOption, draftProblem, type QuestionDraft } from './questionDraft'
import { MIN_OPTIONS, QUESTION_TYPES, questionTypeMeta } from './questionType'
import styles from './QuestionCard.module.css'

/**
 * 질문 한 건의 카드입니다. 편집 가능한 설문지(DRAFT)에서는 **모드 전환 없이 바로 편집**되고, 발행·마감된
 * 설문지에서는 값만 보여 줍니다.
 *
 * <p>«편집» 버튼을 눌러야 고칠 수 있던 이전 구조를 없앤 이유는, 오타 하나를 고치는 데 모드 전환이
 * 필요했고 한 번에 한 문항만 열 수 있었기 때문입니다. 대신 저장은 여전히 **카드 단위**입니다 —
 * 서버 API 가 문항 단위라 여러 문항을 한 번에 보내면 중간에 하나가 400 일 때 어디까지 저장되었는지
 * 알 수 없습니다.
 *
 * <p>편집 중인 값은 상위(빌더)가 보관합니다. 빠른 추가가 아직 저장되지 않은 카드의 유형을 바깥에서
 * 바꿔야 하고, 저장 성공 시 서버가 다듬은 값으로 되돌려 놓아야 하기 때문입니다.
 */
export default function QuestionCard({
  question,
  draft,
  index,
  total,
  editable,
  saving,
  error,
  dirty,
  autoFocus = false,
  onDraftChange,
  onSave,
  onRevert,
  onDelete,
  onMove,
}: {
  /** 저장된 질문입니다. 아직 저장하지 않은 새 카드면 null 입니다. */
  question: QuestionResponse | null
  draft: QuestionDraft
  index: number
  total: number
  editable: boolean
  saving: boolean
  error: ApiError | null
  dirty: boolean
  /** 방금 추가된 카드만 true 입니다. 여러 새 카드가 동시에 요구하면 포커스가 어디로 갈지 모릅니다. */
  autoFocus?: boolean
  onDraftChange: (draft: QuestionDraft) => void
  onSave: () => void
  onRevert: () => void
  onDelete: () => void
  onMove: (direction: -1 | 1) => void
}) {
  const meta = questionTypeMeta(draft.type)
  const isNew = question === null

  if (!editable && question) {
    return <ReadOnlyCard question={question} index={index} />
  }

  const problem = draftProblem(draft)
  const cardClass = isNew ? styles.cardNew : dirty ? styles.cardDirty : ''

  return (
    <article className={`card ${styles.card} ${cardClass}`} data-question-card={question?.id ?? 'new'}>
      <div className={styles.head}>
        <span className={styles.index}>{index + 1}</span>

        {/* 순서 변경은 저장된 문항에만 있습니다. 아직 서버에 없는 카드는 언제나 맨 끝입니다. */}
        {!isNew && (
          <div className={styles.moveGroup}>
            <button
              type="button"
              className={styles.iconButton}
              onClick={() => onMove(-1)}
              disabled={index === 0 || saving}
              aria-label="위로 이동"
              title="위로 이동"
            >
              <ChevronUp size={16} />
            </button>
            <button
              type="button"
              className={styles.iconButton}
              onClick={() => onMove(1)}
              disabled={index === total - 1 || saving}
              aria-label="아래로 이동"
              title="아래로 이동"
            >
              <ChevronDown size={16} />
            </button>
          </div>
        )}

        <div className={styles.headRight}>
          <span className={styles.typePicker}>
            <meta.icon size={15} />
            <select
              className={styles.typeSelect}
              value={draft.type}
              // 유형만 바꾸고 입력값은 남깁니다. 잘못 골랐다 되돌리는 일이 흔한데, 그때마다 제목과
              // 선택지가 사라지면 처음부터 다시 써야 합니다. 저장 시점에 해당 없는 값은 걸러집니다.
              onChange={(e) => onDraftChange({ ...draft, type: e.target.value as QuestionType })}
              disabled={saving}
              aria-label="질문 유형"
            >
              {QUESTION_TYPES.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </span>

          <button
            type="button"
            className={`${styles.iconButton} ${styles.danger}`}
            onClick={onDelete}
            disabled={saving}
            aria-label={isNew ? '새 질문 취소' : '질문 삭제'}
            title={isNew ? '취소' : '삭제'}
          >
            {isNew ? <X size={16} /> : <Trash2 size={16} />}
          </button>
        </div>
      </div>

      {error && (
        <div style={{ marginBottom: 14 }}>
          <ErrorBanner message={error.message} traceId={error.traceId} />
        </div>
      )}

      <input
        type="text"
        className={`${styles.titleInput} ${error?.fieldErrors.title ? styles.invalid : ''}`}
        placeholder="질문을 입력하세요"
        value={draft.title}
        onChange={(e) => onDraftChange({ ...draft, title: e.target.value })}
        disabled={saving}
        maxLength={500}
        // 빠른 추가로 만든 카드는 곧바로 입력할 수 있어야 합니다.
        autoFocus={autoFocus}
      />
      {error?.fieldErrors.title && <p className="field-error">{error.fieldErrors.title}</p>}
      <p className={styles.hint}>응답 화면: {meta.hint}</p>

      {meta.isChoice && (
        <div className={styles.section}>
          <p className={styles.sectionLabel}>선택지</p>
          <div className={styles.options}>
            {draft.options.map((option, optionIndex) => (
              <div key={option.key} className={styles.optionRow}>
                <span className={styles.optionBullet}>•</span>
                <input
                  type="text"
                  className={`input-field ${styles.optionInput}`}
                  placeholder={`선택지 ${optionIndex + 1}`}
                  value={option.label}
                  onChange={(e) =>
                    onDraftChange({
                      ...draft,
                      options: draft.options.map((item) =>
                        item.key === option.key ? { ...item, label: e.target.value } : item,
                      ),
                    })
                  }
                  disabled={saving}
                  maxLength={255}
                  aria-label={`선택지 ${optionIndex + 1}`}
                />
                <button
                  type="button"
                  className={`${styles.iconButton} ${styles.danger}`}
                  onClick={() =>
                    onDraftChange({
                      ...draft,
                      options: draft.options.filter((item) => item.key !== option.key),
                    })
                  }
                  // 최소 개수까지 줄어들면 지우지 못하게 합니다. 지운 뒤 저장이 막히는 것보다
                  // 지울 수 없다는 것이 먼저 보이는 편이 낫습니다.
                  disabled={saving || draft.options.length <= MIN_OPTIONS}
                  aria-label={`선택지 ${optionIndex + 1} 삭제`}
                >
                  <Trash2 size={14} />
                </button>
              </div>
            ))}
          </div>
          <button
            type="button"
            className="btn btn-secondary"
            style={{ marginTop: 10, padding: '6px 12px', fontSize: '0.82rem' }}
            onClick={() => onDraftChange(addOption(draft))}
            disabled={saving}
          >
            <Plus size={13} />
            선택지 추가
          </button>
          <p className={styles.hint}>선택지는 {MIN_OPTIONS}개 이상이어야 저장됩니다.</p>
        </div>
      )}

      {meta.hasRange && (
        <div className={styles.section}>
          <div className={styles.rangeRow}>
            <div className={styles.rangeField}>
              <label className={styles.rangeLabel} htmlFor={`min-${question?.id ?? 'new'}`}>
                최솟값
              </label>
              <input
                id={`min-${question?.id ?? 'new'}`}
                type="number"
                className="input-field"
                placeholder="예: 1"
                value={draft.minValue}
                onChange={(e) => onDraftChange({ ...draft, minValue: e.target.value })}
                disabled={saving}
              />
            </div>
            <div className={styles.rangeField}>
              <label className={styles.rangeLabel} htmlFor={`max-${question?.id ?? 'new'}`}>
                최댓값
              </label>
              <input
                id={`max-${question?.id ?? 'new'}`}
                type="number"
                className="input-field"
                placeholder="예: 5"
                value={draft.maxValue}
                onChange={(e) => onDraftChange({ ...draft, maxValue: e.target.value })}
                disabled={saving}
              />
            </div>
          </div>
          <p className={styles.hint}>
            {draft.type === 'RATING'
              ? '비워 두면 응답 화면이 1~5 척도로 표시합니다.'
              : '비워 두면 값을 제한하지 않습니다.'}
          </p>
        </div>
      )}

      <div className={styles.footer}>
        <label className={styles.checkbox}>
          <input
            type="checkbox"
            checked={draft.required}
            onChange={(e) => onDraftChange({ ...draft, required: e.target.checked })}
            disabled={saving}
          />
          필수 응답
        </label>

        <div className={styles.footerActions}>
          {/* 저장할 수 없는 이유는 저장 버튼 옆에 둡니다 — 눌러 보고 알게 하지 않습니다. */}
          {(dirty || isNew) && problem && <span className={styles.problem}>{problem}</span>}
          {dirty && !isNew && (
            <button type="button" className="btn btn-secondary" onClick={onRevert} disabled={saving}>
              되돌리기
            </button>
          )}
          {(dirty || isNew) && (
            <button
              type="button"
              className="btn btn-primary"
              onClick={onSave}
              disabled={saving || problem !== null}
            >
              {saving && <Spinner size={14} />}
              저장
            </button>
          )}
        </div>
      </div>
    </article>
  )
}

/** 발행·마감된 설문지의 질문입니다. 서버가 409 로 막을 편집 수단을 아예 두지 않습니다. */
function ReadOnlyCard({ question, index }: { question: QuestionResponse; index: number }) {
  const meta = questionTypeMeta(question.type)

  return (
    <article className={`card ${styles.card}`}>
      <div className={styles.head} style={{ marginBottom: 0 }}>
        <span className={styles.index}>{index + 1}</span>
        <div style={{ flex: 1, minWidth: 0 }}>
          <p className={styles.readonlyTitle}>
            {question.title}
            {question.required && <span className={styles.required}>*</span>}
          </p>
          <p className={styles.readonlyMeta}>
            {meta.label}
            {meta.hasRange && (question.minValue !== null || question.maxValue !== null) && (
              <>
                {' '}
                · {question.minValue ?? '제한 없음'} ~ {question.maxValue ?? '제한 없음'}
              </>
            )}
            {meta.isChoice && <> · 선택지 {question.options.length}개</>}
          </p>
        </div>
      </div>

      {meta.isChoice && question.options.length > 0 && (
        <ul className={styles.readonlyOptions}>
          {question.options.map((option) => (
            <li key={option.id}>{option.label}</li>
          ))}
        </ul>
      )}
    </article>
  )
}
