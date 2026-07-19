import { createContext } from 'react'

/**
 * 저장하지 않은 변경을 안고 화면을 떠나려 할 때 한 번 멈춰 세우기 위한 통로입니다.
 *
 * <p>`useBlocker` 를 쓰지 않은 이유는 이 앱이 `BrowserRouter` + `<Routes>` 구성이기 때문입니다.
 * `useBlocker` 는 데이터 라우터(`createBrowserRouter`) 전용이고, 그리로 옮기려면 라우터가
 * `AuthProvider`·`ToastProvider` 조립의 바깥으로 나와야 해서 인증 리다이렉트까지 손대야 합니다.
 * 그래서 <b>탭·목록 링크의 클릭을 가로채는</b> 방식을 씁니다.
 */
export interface UnsavedChanges {
  /** 저장하지 않은 변경 건수를 알립니다. 0 이면 이동을 막지 않습니다. */
  report: (count: number) => void
  /** 이동을 요청합니다. 변경이 없으면 즉시, 있으면 사용자가 확인한 뒤에 `go` 가 실행됩니다. */
  requestLeave: (go: () => void) => void
}

export const UnsavedChangesContext = createContext<UnsavedChanges | null>(null)
