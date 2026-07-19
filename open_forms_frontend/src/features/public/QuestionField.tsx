import type { PublicQuestionResponse } from '../../types/api'
import { questionTypeMeta } from '../forms/questionType'
import type { AnswerValue } from './answerDraft'
import styles from './QuestionField.module.css'

/**
 * 질문 하나의 입력 위젯입니다. 타입에 따라 위젯이 달라지는 것이 전부이며, 분류는 빌더와 같은
 * `questionTypeMeta` 를 씁니다(선택형인지·범위를 쓰는지).
 */
export default function QuestionField({
  question,
  index,
  value,
  onChange,
  error,
  disabled,
}: {
  question: PublicQuestionResponse
  index: number
  value: AnswerValue
  onChange: (value: AnswerValue) => void
  error?: string
  disabled: boolean
}) {
  const meta = questionTypeMeta(question.type)
  const fieldId = `question-${question.id}`

  return (
    <fieldset
      className={`card ${styles.field} ${error ? styles.fieldError : ''}`}
      id={fieldId}
      // 오류가 난 문항으로 초점을 옮길 수 있게 합니다(제출 시 스크롤 대상).
      tabIndex={-1}
    >
      <legend className={styles.title}>
        {index + 1}. {question.title}
        {question.required && (
          <span className={styles.required} aria-label="필수">
            *
          </span>
        )}
      </legend>
      <p className={styles.hint}>{meta.label}</p>

      {renderInput()}

      {error && (
        <span className={styles.error} role="alert">
          {error}
        </span>
      )}
    </fieldset>
  )

  function renderInput() {
    if (meta.isChoice && value.kind === 'options') {
      return question.type === 'DROPDOWN' ? renderDropdown() : renderChoices()
    }
    if (value.kind !== 'text') {
      return null
    }
    switch (question.type) {
      case 'LONG_TEXT':
        return (
          <textarea
            className="input-field"
            rows={4}
            value={value.value}
            onChange={(e) => onChange({ kind: 'text', value: e.target.value })}
            disabled={disabled}
            aria-label={question.title}
          />
        )
      case 'RATING':
        return renderScale()
      case 'NUMBER':
        return (
          <input
            type="number"
            className="input-field"
            value={value.value}
            min={question.minValue ?? undefined}
            max={question.maxValue ?? undefined}
            onChange={(e) => onChange({ kind: 'text', value: e.target.value })}
            disabled={disabled}
            aria-label={question.title}
          />
        )
      case 'DATE':
        return (
          <input
            type="date"
            className="input-field"
            value={value.value}
            onChange={(e) => onChange({ kind: 'text', value: e.target.value })}
            disabled={disabled}
            aria-label={question.title}
          />
        )
      case 'TIME':
        return (
          <input
            type="time"
            className="input-field"
            value={value.value}
            onChange={(e) => onChange({ kind: 'text', value: e.target.value })}
            disabled={disabled}
            aria-label={question.title}
          />
        )
      default:
        return (
          <input
            type="text"
            className="input-field"
            value={value.value}
            onChange={(e) => onChange({ kind: 'text', value: e.target.value })}
            disabled={disabled}
            aria-label={question.title}
          />
        )
    }
  }

  function renderChoices() {
    if (value.kind !== 'options') {
      return null
    }
    const multiple = question.type === 'MULTIPLE_CHOICE'
    const selected = value.optionIds

    return (
      <div className={styles.choices}>
        {question.options.map((option) => {
          const checked = selected.includes(option.id)
          return (
            <label
              key={option.id}
              className={`${styles.choice} ${checked ? styles.choiceSelected : ''}`}
            >
              <input
                type={multiple ? 'checkbox' : 'radio'}
                // 라디오는 같은 name 을 공유해야 브라우저가 하나만 선택되게 관리합니다.
                name={`question-${question.id}-option`}
                checked={checked}
                onChange={() => {
                  if (!multiple) {
                    onChange({ kind: 'options', optionIds: [option.id] })
                    return
                  }
                  onChange({
                    kind: 'options',
                    optionIds: checked
                      ? selected.filter((id) => id !== option.id)
                      : [...selected, option.id],
                  })
                }}
                disabled={disabled}
              />
              {option.label}
            </label>
          )
        })}
      </div>
    )
  }

  function renderDropdown() {
    if (value.kind !== 'options') {
      return null
    }
    return (
      <select
        className="input-field"
        value={value.optionIds[0] ?? ''}
        onChange={(e) =>
          onChange({
            kind: 'options',
            optionIds: e.target.value === '' ? [] : [Number(e.target.value)],
          })
        }
        disabled={disabled}
        aria-label={question.title}
      >
        <option value="">선택해 주세요</option>
        {question.options.map((option) => (
          <option key={option.id} value={option.id}>
            {option.label}
          </option>
        ))}
      </select>
    )
  }

  function renderScale() {
    if (value.kind !== 'text') {
      return null
    }
    // 범위는 서버에서 선택 사항이라 둘 다 null 일 수 있습니다. 척도 버튼은 범위 없이는 그릴 수
    // 없으므로 흔한 5점 척도를 기본값으로 씁니다.
    const min = question.minValue ?? 1
    const max = question.maxValue ?? 5
    const steps = Array.from({ length: Math.max(0, max - min + 1) }, (_, i) => min + i)

    // 범위가 지나치게 넓으면 버튼이 화면을 덮으므로 숫자 입력으로 물러납니다.
    if (steps.length === 0 || steps.length > 11) {
      return (
        <input
          type="number"
          className="input-field"
          value={value.value}
          min={min}
          max={max}
          onChange={(e) => onChange({ kind: 'text', value: e.target.value })}
          disabled={disabled}
          aria-label={question.title}
        />
      )
    }

    return (
      <>
        <div className={styles.scale} role="radiogroup" aria-label={question.title}>
          {steps.map((step) => {
            const selected = value.value === String(step)
            return (
              <button
                key={step}
                type="button"
                role="radio"
                aria-checked={selected}
                className={`${styles.scaleButton} ${selected ? styles.scaleSelected : ''}`}
                onClick={() =>
                  // 같은 값을 다시 누르면 선택을 해제합니다. 선택 문항이라면 실수로 누른 뒤
                  // 되돌릴 방법이 있어야 합니다.
                  onChange({ kind: 'text', value: selected ? '' : String(step) })
                }
                disabled={disabled}
              >
                {step}
              </button>
            )
          })}
        </div>
        <div className={styles.scaleEnds}>
          <span>{min} (낮음)</span>
          <span>{max} (높음)</span>
        </div>
      </>
    )
  }
}
