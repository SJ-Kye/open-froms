import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  LabelList,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { QuestionStats } from '../../types/api'
import { questionTypeMeta } from '../forms/questionType'
import ChartTooltip from './ChartTooltip'
import { AXIS_FONT_SIZE, BAR_RADIUS, useChartColors } from './chartTheme'
import styles from './DashboardPage.module.css'

/**
 * 질문 하나의 응답 분포입니다. 타입마다 서버가 채워 주는 필드가 달라 표현도 갈립니다.
 *
 * <ul>
 *   <li>선택형 → 가로 막대(선택지별 선택 수). 라벨이 길고 비교가 목적이라 파이 대신 막대입니다 —
 *       파이는 각도 비교라 조각이 셋만 넘어도 순위를 읽기 어렵습니다.</li>
 *   <li>평점·숫자 → 세로 막대(값별 개수) + 평균. 값 자체가 순서 있는 축입니다.</li>
 *   <li>텍스트형 → 최근 응답 목록(서버가 최대 5건).</li>
 *   <li>날짜·시각 → 응답 수만(서버가 그것만 채웁니다).</li>
 * </ul>
 */
export default function QuestionStatsCard({
  stats,
  totalResponses,
}: {
  stats: QuestionStats
  totalResponses: number
}) {
  const colors = useChartColors()
  const meta = questionTypeMeta(stats.type)

  return (
    <article className={`card ${styles.questionCard}`}>
      <h3 className={styles.questionTitle}>
        {stats.title}
        {stats.required && <span style={{ color: 'var(--error)' }}> *</span>}
      </h3>
      <p className={styles.questionMeta}>
        {meta.label} · 응답 {stats.answeredCount}/{totalResponses}
        {stats.average !== null && <> · 평균 {stats.average.toFixed(2)}</>}
      </p>

      {renderBody()}
    </article>
  )

  function renderBody() {
    if (stats.answeredCount === 0 && stats.optionCounts.length === 0) {
      return <p className={styles.noData}>아직 이 문항에 대한 응답이 없습니다.</p>
    }

    if (meta.isChoice) {
      // optionCounts 에는 아무도 고르지 않은 선택지도 0 으로 들어 있습니다(서버 계약). 그대로
      // 그려야 "선택지가 있었지만 아무도 안 골랐다"가 보입니다.
      const data = stats.optionCounts.map((option) => ({ name: option.label, count: option.count }))
      const height = Math.max(120, data.length * 44)
      return (
        <div style={{ width: '100%', height }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data} layout="vertical" margin={{ top: 0, right: 32, bottom: 0, left: 0 }}>
              <CartesianGrid stroke={colors.grid} strokeDasharray="3 3" horizontal={false} />
              <XAxis
                type="number"
                allowDecimals={false}
                tick={{ fill: colors.axis, fontSize: AXIS_FONT_SIZE }}
                axisLine={false}
                tickLine={false}
              />
              <YAxis
                type="category"
                dataKey="name"
                width={110}
                tick={{ fill: colors.text, fontSize: AXIS_FONT_SIZE }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                cursor={{ fill: colors.grid, fillOpacity: 0.35 }}
                content={({ active, payload, label }) => (
                  <ChartTooltip
                    active={active}
                    label={typeof label === 'string' ? label : undefined}
                    value={payload?.[0]?.value as number | undefined}
                    unit="명"
                    colors={colors}
                  />
                )}
              />
              <Bar dataKey="count" fill={colors.series} radius={[0, BAR_RADIUS, BAR_RADIUS, 0]} barSize={18}>
                {/* 색만으로 값을 읽게 하지 않습니다 — 막대 끝에 수를 직접 적습니다. */}
                <LabelList
                  dataKey="count"
                  position="right"
                  fill={colors.text}
                  fontSize={AXIS_FONT_SIZE}
                />
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )
    }

    if (meta.hasRange) {
      const data = stats.valueCounts.map((item) => ({ name: String(item.value), count: item.count }))
      if (data.length === 0) {
        return <p className={styles.noData}>아직 이 문항에 대한 응답이 없습니다.</p>
      }
      return (
        <div style={{ width: '100%', height: 200 }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data} margin={{ top: 16, right: 8, bottom: 0, left: -20 }}>
              <CartesianGrid stroke={colors.grid} strokeDasharray="3 3" vertical={false} />
              <XAxis
                dataKey="name"
                tick={{ fill: colors.text, fontSize: AXIS_FONT_SIZE }}
                axisLine={{ stroke: colors.grid }}
                tickLine={false}
              />
              <YAxis
                allowDecimals={false}
                tick={{ fill: colors.axis, fontSize: AXIS_FONT_SIZE }}
                axisLine={false}
                tickLine={false}
                width={44}
              />
              <Tooltip
                cursor={{ fill: colors.grid, fillOpacity: 0.35 }}
                content={({ active, payload, label }) => (
                  <ChartTooltip
                    active={active}
                    label={typeof label === 'string' ? `${label}점` : undefined}
                    value={payload?.[0]?.value as number | undefined}
                    unit="명"
                    colors={colors}
                  />
                )}
              />
              <Bar dataKey="count" radius={[BAR_RADIUS, BAR_RADIUS, 0, 0]} maxBarSize={48}>
                {data.map((item) => (
                  <Cell key={item.name} fill={colors.series} />
                ))}
                <LabelList
                  dataKey="count"
                  position="top"
                  fill={colors.text}
                  fontSize={AXIS_FONT_SIZE}
                />
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )
    }

    if (stats.recentTexts.length > 0) {
      return (
        <>
          <div className={styles.texts}>
            {stats.recentTexts.map((text, index) => (
              <p key={index} className={styles.textItem}>
                {text}
              </p>
            ))}
          </div>
          {/* 서버가 최근 5건만 내려 줍니다. 그보다 많으면 어디서 전문을 보는지 알려 줍니다. */}
          {stats.answeredCount > stats.recentTexts.length && (
            <p className={styles.questionMeta} style={{ marginTop: 12, marginBottom: 0 }}>
              최근 {stats.recentTexts.length}건만 표시합니다. 전문은 «응답» 탭에서 확인하세요.
            </p>
          )}
        </>
      )
    }

    // DATE·TIME 은 서버가 answeredCount 만 채웁니다. 값의 분포를 그리려면 별도 집계가 필요합니다.
    return <p className={styles.noData}>{stats.answeredCount}건의 응답이 있습니다.</p>
  }
}
