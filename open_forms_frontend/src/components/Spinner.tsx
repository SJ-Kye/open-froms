/** 로딩 표시입니다. `page` 는 화면 전체를 채우는 자리(라우트 전환·인증 확인)에 씁니다. */
export default function Spinner({ size = 20, page = false }: { size?: number; page?: boolean }) {
  const spinner = (
    <span
      className="spinner"
      style={{ width: size, height: size }}
      role="status"
      aria-label="불러오는 중"
    />
  )
  return page ? <div className="spinner-page">{spinner}</div> : spinner
}
