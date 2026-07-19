import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { DailyCount } from '../../types/api'
import ChartTooltip from './ChartTooltip'
import { AXIS_FONT_SIZE, useChartColors } from './chartTheme'
import styles from './DashboardPage.module.css'

/**
 * 일별 응답 추이입니다.
 *
 * <p>서버가 **발행일부터 종료일(미종료면 오늘)까지 빈 날을 0 으로 채워** 보내 주므로 화면에서 날짜를
 * 메우지 않습니다. 빈 날을 건너뛰면 3일 간격과 하루 간격이 같은 폭으로 그려져 추이가 거짓말을 합니다.
 *
 * <p>계열이 하나라 범례를 두지 않습니다 — 제목이 계열을 지칭합니다.
 */
export default function ResponsesTrendChart({ data }: { data: DailyCount[] }) {
  const colors = useChartColors()

  return (
    <div className={styles.chartBox}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: -16 }}>
          {/* 그리드는 가로선만 둡니다. 값을 읽는 데 쓰이는 것은 y 눈금이고, 세로선은 잉크만 늘립니다. */}
          <CartesianGrid stroke={colors.grid} strokeDasharray="3 3" vertical={false} />
          <XAxis
            dataKey="date"
            tickFormatter={formatTick}
            tick={{ fill: colors.axis, fontSize: AXIS_FONT_SIZE }}
            axisLine={{ stroke: colors.grid }}
            tickLine={false}
            minTickGap={24}
          />
          <YAxis
            allowDecimals={false}
            tick={{ fill: colors.axis, fontSize: AXIS_FONT_SIZE }}
            axisLine={false}
            tickLine={false}
            width={44}
          />
          <Tooltip
            cursor={{ stroke: colors.axis, strokeWidth: 1 }}
            content={({ active, payload, label }) => (
              <ChartTooltip
                active={active}
                label={typeof label === 'string' ? formatFullDate(label) : undefined}
                value={payload?.[0]?.value as number | undefined}
                unit="건"
                colors={colors}
              />
            )}
          />
          <Line
            type="monotone"
            dataKey="count"
            stroke={colors.series}
            strokeWidth={2}
            // 점이 매일 찍히면 선이 보이지 않습니다. 기본은 숨기고 가리킨 지점만 크게 보여 줍니다.
            dot={false}
            activeDot={{ r: 5, strokeWidth: 2, stroke: colors.surface }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}

/** 축은 `07-19` 처럼 짧게 — 연도는 모든 눈금에서 같은 값이라 자리만 차지합니다. */
function formatTick(value: string): string {
  return value.slice(5)
}

function formatFullDate(value: string): string {
  const [year, month, day] = value.split('-')
  return `${year}년 ${Number(month)}월 ${Number(day)}일`
}
