import type { FormStatus } from '../../types/api'
import { STATUS_LABELS } from './formStatus'
import styles from './StatusBadge.module.css'

/**
 * 설문지 상태 배지입니다. 색은 상태의 성격을 따릅니다 — 작성 중은 중립, 공개 중은 응답을 받고 있다는
 * 뜻이라 성공색, 종료는 더 이상 받지 않으므로 경고색입니다.
 */
export default function StatusBadge({ status }: { status: FormStatus }) {
  return <span className={`${styles.badge} ${styles[status.toLowerCase()]}`}>{STATUS_LABELS[status]}</span>
}
