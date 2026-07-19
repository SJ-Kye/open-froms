import { useEffect, useState } from 'react'

/**
 * 차트가 쓸 색을 CSS 토큰에서 읽어 옵니다. Recharts 는 색을 **문자열 prop** 으로 받기 때문에
 * `var(--...)` 를 그대로 넘길 수 없는 자리(그라데이션 stop, 툴팁 커서 등)가 있어, 실제 계산값이
 * 필요합니다.
 *
 * <p>다크 모드로 바뀌면 `<html>` 의 클래스가 바뀌므로 그때 다시 읽습니다. 그러지 않으면 테마를
 * 전환해도 차트만 이전 색으로 남습니다.
 */

export interface ChartColors {
  /** 계열(막대·선) 색입니다. 밝기·대비 검증을 통과한 전용 값입니다(tokens.css 참고). */
  series: string
  grid: string
  axis: string
  text: string
  surface: string
  border: string
}

function readColors(): ChartColors {
  const style = getComputedStyle(document.documentElement)
  const value = (name: string) => style.getPropertyValue(name).trim()
  return {
    series: value('--chart-series'),
    grid: value('--border-color'),
    axis: value('--text-muted'),
    text: value('--text-secondary'),
    surface: value('--bg-secondary'),
    border: value('--border-color'),
  }
}

export function useChartColors(): ChartColors {
  const [colors, setColors] = useState<ChartColors>(readColors)

  useEffect(() => {
    const observer = new MutationObserver(() => setColors(readColors()))
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
    return () => observer.disconnect()
  }, [])

  return colors
}

/** 축·툴팁 글자 크기입니다. 차트마다 흩어지지 않게 한곳에 둡니다. */
export const AXIS_FONT_SIZE = 12

/** 막대 데이터 끝의 라운드 반경입니다(4px, 축에 닿는 쪽은 각지게). */
export const BAR_RADIUS = 4
