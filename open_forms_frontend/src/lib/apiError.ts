import { AxiosError } from 'axios'
import type { ErrorResponse, FieldError } from '../types/api'

/**
 * 화면이 그대로 쓸 수 있게 정리한 실패 정보입니다.
 *
 * @property code        서버의 에러 코드(`INVALID_CREDENTIALS` 등). 화면이 특정 실패만 다르게
 *                       처리해야 할 때 문구 대신 이 값으로 분기합니다.
 * @property message     사용자에게 보여 줄 문구입니다.
 * @property fieldErrors 필드명 → 사유. 입력칸 옆에 바로 붙일 수 있는 형태로 폅니다.
 * @property traceId     서버 로그 대조용 식별자입니다.
 */
export interface ApiError {
  status: number | null
  code: string
  message: string
  fieldErrors: Record<string, string>
  traceId?: string
}

const NETWORK_MESSAGE = '서버에 연결할 수 없습니다. 네트워크 상태를 확인해 주세요.'
const UNKNOWN_MESSAGE = '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'

function toFieldMap(fieldErrors: FieldError[] | undefined): Record<string, string> {
  const map: Record<string, string> = {}
  // 같은 필드에 사유가 여럿이면 첫 번째만 씁니다. 입력칸 하나에 문구를 쌓아 봐야
  // 사용자가 먼저 고칠 것은 어차피 하나입니다.
  for (const { field, message } of fieldErrors ?? []) {
    if (!(field in map)) {
      map[field] = message
    }
  }
  return map
}

/**
 * axios 에러를 화면용 정보로 바꿉니다.
 *
 * <p>서버가 보낸 `message` 를 그대로 씁니다. 실패 문구를 프론트에서 다시 쓰면 같은 규칙이 두 곳에
 * 존재하게 되고(예: 계정 열거를 막으려고 로그인 실패를 뭉뚱그린 서버의 의도가 프론트 문구에서
 * 깨질 수 있습니다), 백엔드가 문구를 고쳐도 화면이 따라가지 않습니다.
 */
export function toApiError(error: unknown): ApiError {
  if (error instanceof AxiosError) {
    const data = error.response?.data as ErrorResponse | undefined
    if (data?.message) {
      return {
        status: data.status ?? error.response?.status ?? null,
        code: data.code,
        message: data.message,
        fieldErrors: toFieldMap(data.fieldErrors),
        traceId: data.traceId,
      }
    }
    // 응답 자체가 없으면 네트워크 단절·CORS·서버 다운입니다. 이때는 서버 문구가 없습니다.
    if (!error.response) {
      return { status: null, code: 'NETWORK_ERROR', message: NETWORK_MESSAGE, fieldErrors: {} }
    }
    return {
      status: error.response.status,
      code: 'UNKNOWN',
      message: UNKNOWN_MESSAGE,
      fieldErrors: {},
    }
  }
  return { status: null, code: 'UNKNOWN', message: UNKNOWN_MESSAGE, fieldErrors: {} }
}
