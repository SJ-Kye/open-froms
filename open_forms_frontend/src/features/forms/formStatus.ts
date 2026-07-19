import type { FormStatus } from '../../types/api'

/** 설문지 상태의 한국어 표기입니다. 배지와 안내 문구가 같은 말을 쓰도록 한곳에 둡니다. */
export const STATUS_LABELS: Record<FormStatus, string> = {
  DRAFT: '작성 중',
  PUBLISHED: '공개 중',
  CLOSED: '종료됨',
}

/**
 * 현재 상태에서 넘어갈 수 있는 다음 상태입니다. 전이는 `DRAFT → PUBLISHED → CLOSED` 단방향이라
 * 각 상태의 다음은 하나뿐이고, 종료 이후에는 없습니다(서버가 되돌리는 전이를 409 로 거부합니다).
 */
export function nextStatus(status: FormStatus): FormStatus | null {
  if (status === 'DRAFT') return 'PUBLISHED'
  if (status === 'PUBLISHED') return 'CLOSED'
  return null
}
