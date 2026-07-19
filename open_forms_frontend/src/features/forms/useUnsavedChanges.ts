import { useContext, useEffect } from 'react'
import { UnsavedChangesContext, type UnsavedChanges } from './unsavedChangesContext'

export function useUnsavedChanges(): UnsavedChanges {
  const value = useContext(UnsavedChangesContext)
  if (!value) {
    throw new Error('useUnsavedChanges 는 FormTabsLayout 안에서만 쓸 수 있습니다.')
  }
  return value
}

/**
 * 저장하지 않은 변경 건수를 상위에 보고합니다. 화면을 떠날 때 0 으로 되돌리는 정리가 핵심입니다 —
 * 빠뜨리면 다른 탭으로 옮긴 뒤에도 «변경이 있다»는 상태가 남아 이동할 때마다 확인창이 뜹니다.
 */
export function useReportUnsavedChanges(count: number) {
  const { report } = useUnsavedChanges()
  useEffect(() => {
    report(count)
    return () => report(0)
  }, [count, report])
}
