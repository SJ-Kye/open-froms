import { Plus, X } from 'lucide-react'
import Spinner from '../../components/Spinner'
import ErrorBanner from '../../components/ErrorBanner'
import type { ApiError } from '../../lib/apiError'
import {
  addOption,
  draftProblem,
  type OptionDraft,
  type QuestionDraft,
} from './questionDraft'
import { MIN_OPTIONS, QUESTION_TYPES, questionTypeMeta } from './questionType'
import type { QuestionType } from '../../types/api'
import styles from './QuestionEditor.module.css'

/**
 * 질문 하나를 편집합니다. 유형에 따라 입력 항목이 달라지는 것이 이 컴포넌트의 전부입니다 —
 * 선택형은 선택지 목록을, 평점·숫자는 범위를 보여 주고, 나머지는 제목만 받습니다.
 *
 * <p>저장은 질문 단위입니다(서버 API 가 질문 단위이므로). 여러 질문을 모아 한 번에 보내면 중간에
 * 하나가 400 일 때 어디까지 저장되었는지 사용자가 알 수 없습니다.
 */
export default function QuestionEditor({
  draft,
  onChange,
  onSave,
  onCancel,
  saving,
  error,
}: {
  draft: QuestionDraft
  onChange: (draft: QuestionDraft) => void
  onSave: () => void
  onCancel: () => void
  saving: boolean
  error: ApiError | null
}) {
  const meta = questionTypeMeta(draft.type)
  const problem = draftProblem(draft)

  function changeType(type: QuestionType) {
    // 유형만 바꾸고 입력값은 남겨 둡니다. 잘못 골랐다가 되돌리는 일이 흔한데, 그때마다 제목과
    // 선택지가 사라지면 처음부터 다시 써야 합니다. 저장 시점에 해당 없는 값은 걸러집니다.
    onChange({ ...draft, type })
  }

  function changeOption(key: string, label: string) {
    onChange({
      ...draft,
      options: draft.options.map((option) => (option.key === key ? { ...option, label } : option)),
    })
  }

  function removeOption(key: string) {
    onChange({ ...draft, options: draft.options.filter((option) => option.key !== key) })
  }

  return (
    <div className={styles.editor}>
      {error && (
        <div style={{ marginBottom: 16 }}>
          <ErrorBanner message={error.message} traceId={error.traceId} />
        </div>
      )}

      <div className="form-group">
        <label className="form-label" htmlFor="question-title">
          질문
        </label>
        <input
          id="question-title"
          type="text"
          className={`input-field ${error?.fieldErrors.title ? 'has-error' : ''}`}
          placeholder="예: 서비스에 만족하시나요?"
          value={draft.title}
          onChange={(e) => onChange({ ...draft, title: e.target.value })}
          disabled={saving}
          maxLength={500}
          autoFocus
        />
        {error?.fieldErrors.title && <span className="field-error">{error.fieldErrors.title}</span>}
      </div>

      <div className={styles.row}>
        <div className="form-group">
          <label className="form-label" htmlFor="question-type">
            유형
          </label>
          <select
            id="question-type"
            className="input-field"
            value={draft.type}
            onChange={(e) => changeType(e.target.value as QuestionType)}
            disabled={saving}
          >
            {QUESTION_TYPES.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
          <span className={styles.typeHint}>응답 화면: {meta.hint}</span>
        </div>

        {meta.hasRange && (
          <>
            <div className="form-group">
              <label className="form-label" htmlFor="question-min">
                최솟값
              </label>
              <input
                id="question-min"
                type="number"
                className="input-field"
                placeholder="예: 1"
                value={draft.minValue}
                onChange={(e) => onChange({ ...draft, minValue: e.target.value })}
                disabled={saving}
              />
            </div>
            <div className="form-group">
              <label className="form-label" htmlFor="question-max">
                최댓값
              </label>
              <input
                id="question-max"
                type="number"
                className="input-field"
                placeholder="예: 5"
                value={draft.maxValue}
                onChange={(e) => onChange({ ...draft, maxValue: e.target.value })}
                disabled={saving}
              />
            </div>
          </>
        )}
      </div>

      {/* 범위는 선택 사항입니다(서버도 둘 다 있을 때만 대소를 검증). 그 사실을 알려 둡니다. */}
      {meta.hasRange && (
        <p className={styles.typeHint} style={{ marginTop: -8, marginBottom: 16 }}>
          범위를 비워 두면 값을 제한하지 않습니다.
        </p>
      )}

      {meta.isChoice && (
        <div className="form-group">
          <label className="form-label">선택지 (최소 {MIN_OPTIONS}개)</label>
          <div className={styles.options}>
            {draft.options.map((option: OptionDraft, index) => (
              <div key={option.key} className={styles.optionRow}>
                <span className={styles.optionIndex}>{index + 1}.</span>
                <input
                  type="text"
                  className="input-field"
                  placeholder={`선택지 ${index + 1}`}
                  value={option.label}
                  onChange={(e) => changeOption(option.key, e.target.value)}
                  disabled={saving}
                  maxLength={255}
                  aria-label={`선택지 ${index + 1}`}
                />
                <button
                  type="button"
                  className={styles.optionRemove}
                  onClick={() => removeOption(option.key)}
                  // 최소 개수까지 줄어들면 지우지 못하게 합니다. 지운 뒤 저장 버튼이 막히는 것보다
                  // 지울 수 없다는 것이 먼저 보이는 편이 낫습니다.
                  disabled={saving || draft.options.length <= MIN_OPTIONS}
                  aria-label={`선택지 ${index + 1} 삭제`}
                >
                  <X size={16} />
                </button>
              </div>
            ))}
          </div>
          <button
            type="button"
            className="btn btn-secondary"
            style={{ alignSelf: 'flex-start', marginTop: 4 }}
            onClick={() => onChange(addOption(draft))}
            disabled={saving}
          >
            <Plus size={15} />
            선택지 추가
          </button>
        </div>
      )}

      <label className={styles.checkbox}>
        <input
          type="checkbox"
          checked={draft.required}
          onChange={(e) => onChange({ ...draft, required: e.target.checked })}
          disabled={saving}
        />
        필수 응답
      </label>

      <div className={styles.footer}>
        {problem && <span className={styles.problem}>{problem}</span>}
        <div className={styles.footerActions}>
          <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={saving}>
            취소
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={onSave}
            disabled={saving || problem !== null}
          >
            {saving && <Spinner size={14} />}
            저장
          </button>
        </div>
      </div>
    </div>
  )
}
