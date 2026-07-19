import type { ReactNode } from 'react'

/**
 * 목록이 비었을 때의 안내입니다. "데이터 없음"을 빈 화면으로 두면 사용자는 로딩 실패와 구분하지
 * 못하므로, 무엇이 없는지와 다음에 할 일을 함께 보여 줍니다.
 */
export default function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon?: ReactNode
  title: string
  description?: string
  action?: ReactNode
}) {
  return (
    <div className="empty-state">
      {icon && <div className="empty-state-icon">{icon}</div>}
      <p className="empty-state-title">{title}</p>
      {description && <p className="empty-state-desc">{description}</p>}
      {action && <div style={{ marginTop: 24 }}>{action}</div>}
    </div>
  )
}
