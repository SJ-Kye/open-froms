import { Pencil, Trash2 } from 'lucide-react'
import type { QuestionResponse } from '../../types/api'
import { questionTypeMeta } from './questionType'
import styles from './QuestionCard.module.css'

/**
 * 저장된 질문 한 건의 요약입니다. 편집 불가 상태(발행·종료된 폼)에서는 버튼 없이 내용만 보여
 * 줍니다 — 서버가 409 로 막을 동작을 화면에 두면 사용자는 눌러 봐야 알 수 있습니다.
 */
export default function QuestionCard({
  question,
  index,
  editable,
  onEdit,
  onDelete,
}: {
  question: QuestionResponse
  index: number
  editable: boolean
  onEdit: () => void
  onDelete: () => void
}) {
  const meta = questionTypeMeta(question.type)

  return (
    <article className={`card ${styles.card}`}>
      <div className={styles.head}>
        <span className={styles.index}>{index + 1}</span>
        <div className={styles.headText}>
          <p className={styles.title}>
            {question.title}
            {question.required && <span className={styles.required}>*</span>}
          </p>
          <p className={styles.meta}>
            {meta.label}
            {meta.hasRange && (question.minValue !== null || question.maxValue !== null) && (
              <> · {question.minValue ?? '제한 없음'} ~ {question.maxValue ?? '제한 없음'}</>
            )}
            {meta.isChoice && <> · 선택지 {question.options.length}개</>}
          </p>
        </div>
        {editable && (
          <div className={styles.actions}>
            <button type="button" className="btn-icon" onClick={onEdit} aria-label="질문 수정">
              <Pencil size={16} />
            </button>
            <button type="button" className="btn-icon" onClick={onDelete} aria-label="질문 삭제">
              <Trash2 size={16} />
            </button>
          </div>
        )}
      </div>

      {meta.isChoice && question.options.length > 0 && (
        <ul className={styles.options}>
          {question.options.map((option) => (
            <li key={option.id}>{option.label}</li>
          ))}
        </ul>
      )}
    </article>
  )
}
