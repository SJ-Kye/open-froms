import { Sparkles } from 'lucide-react'
import type { QuestionType } from '../../types/api'
import { QUESTION_TYPES } from './questionType'
import styles from './QuickAddPanel.module.css'

/**
 * 유형 버튼 하나로 그 유형의 문항을 바로 만듭니다.
 *
 * <p>이전에는 «질문 추가» 가 항상 단답형으로 시작해, 객관식을 만들려면 추가 → 유형 변경 →
 * 선택지 입력의 세 단계를 거쳐야 했습니다. 만들려는 것이 무엇인지는 누르기 전에 이미 정해져
 * 있으므로, 그 정보를 버튼이 받습니다.
 */
export default function QuickAddPanel({
  onAdd,
  activeType,
  disabled,
}: {
  onAdd: (type: QuestionType) => void
  /** 아직 저장하지 않은 카드가 있으면 그 유형입니다. 버튼이 현재 상태를 드러냅니다. */
  activeType: QuestionType | null
  disabled: boolean
}) {
  return (
    <aside className={styles.panel}>
      <div className="card">
        <h2 className={styles.title}>
          <Sparkles size={16} />
          빠른 문항 추가
        </h2>
        <p className={styles.hint}>
          유형을 고르면 그 형태의 문항이 목록 끝에 추가됩니다. 저장하기 전에는 다른 유형을 눌러
          바꿀 수 있습니다.
        </p>
        <div className={styles.buttons}>
          {QUESTION_TYPES.map((meta) => (
            <button
              key={meta.value}
              type="button"
              className={`btn btn-secondary ${styles.typeButton} ${
                activeType === meta.value ? styles.typeButtonActive : ''
              }`}
              onClick={() => onAdd(meta.value)}
              disabled={disabled}
              title={meta.hint}
            >
              <meta.icon size={15} />
              {meta.label}
            </button>
          ))}
        </div>
      </div>
    </aside>
  )
}
