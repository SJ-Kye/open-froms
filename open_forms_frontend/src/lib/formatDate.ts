/**
 * 서버가 주는 `LocalDateTime` 문자열(`2026-07-19T17:54:18.182847`)을 화면용으로 바꿉니다.
 *
 * <p>이 값에는 **오프셋이 없습니다**(`docs/05` 「날짜·시각 표기」). 서버와 브라우저가 같은 시간대인
 * 로컬 실행을 전제로 그대로 로컬 시각으로 해석합니다. 시간대가 다른 환경에 배포한다면 서버가
 * 오프셋을 포함해 내려주도록 바꾸는 편이 맞고, 그 전까지는 여기가 그 가정이 모인 자리입니다.
 */

const DATE_TIME = new Intl.DateTimeFormat('ko-KR', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

const DATE_ONLY = new Intl.DateTimeFormat('ko-KR', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
})

export function formatDateTime(value: string): string {
  return DATE_TIME.format(new Date(value))
}

export function formatDate(value: string): string {
  return DATE_ONLY.format(new Date(value))
}
