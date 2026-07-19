import type { ChartColors } from './chartTheme'
import styles from './DashboardPage.module.css'

/**
 * 차트 공용 툴팁입니다. Recharts 기본 툴팁은 흰 배경이 고정이라 다크 모드에서 눈이 부시고, 글자에
 * 계열 색을 씁니다. 여기서는 배경을 표면 토큰으로 맞추고 **글자는 텍스트 토큰**으로 둡니다 —
 * 계열 색은 옆의 점이 담당합니다.
 */
export default function ChartTooltip({
  active,
  label,
  value,
  unit,
  colors,
}: {
  active?: boolean
  label?: string
  value?: number
  unit: string
  colors: ChartColors
}) {
  if (!active || value === undefined) {
    return null
  }
  return (
    <div
      className={styles.tooltip}
      style={{ background: colors.surface, borderColor: colors.border }}
    >
      <span className={styles.tooltipLabel}>{label}</span>
      <span className={styles.tooltipValue}>
        <i className={styles.tooltipDot} style={{ background: colors.series }} />
        {value.toLocaleString()}
        {unit}
      </span>
    </div>
  )
}
