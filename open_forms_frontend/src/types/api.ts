/**
 * 백엔드 DTO 와 1:1 로 대응하는 타입입니다. 필드명은 서버 record 와 정확히 같아야 하며,
 * 근거는 `docs/05-api-design.md` 의 요청/응답 예시(실행 캡처값)입니다.
 */

/** 검증 실패(400) 시 필드별 사유입니다. */
export interface FieldError {
  field: string
  message: string
}

/**
 * 모든 에러 응답의 공통 포맷입니다(성공 응답은 래퍼 없이 각 API 의 본문 그대로).
 * 서버의 `common/exception/ErrorResponse` 와 대응합니다.
 */
export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  code: string
  message: string
  path: string
  traceId: string
  fieldErrors: FieldError[]
}

/** 로그인·토큰 갱신 응답입니다. `refreshToken` 은 1회용이라 쓸 때마다 새 값으로 회전합니다. */
export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  /** 액세스 토큰 만료까지 남은 초입니다. */
  expiresIn: number
}

/** 사용자 표현입니다(회원가입 결과·`/me` 공용). 비밀번호 해시는 어떤 응답에도 담기지 않습니다. */
export interface UserResponse {
  id: string
  email: string
  name: string
  createdAt: string
}

/** 목록 API 의 공통 페이지 래퍼입니다(서버 `common/response/PageResponse`). */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** 폼 생명주기입니다. 전이는 `DRAFT → PUBLISHED → CLOSED` 단방향이며 되돌릴 수 없습니다. */
export type FormStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED'

/**
 * 질문 유형 9종입니다. 서버 `form/domain/QuestionType` 과 같은 값이며, 값이 어긋나면 400 이 아니라
 * 역직렬화 단계에서 실패하므로 문자열 리터럴 유니온으로 고정합니다.
 */
export type QuestionType =
  | 'SHORT_TEXT'
  | 'LONG_TEXT'
  | 'SINGLE_CHOICE'
  | 'DROPDOWN'
  | 'MULTIPLE_CHOICE'
  | 'RATING'
  | 'NUMBER'
  | 'DATE'
  | 'TIME'

export interface OptionResponse {
  id: number
  label: string
  position: number
}

/**
 * 제작자용 질문 표현입니다.
 *
 * `options` 는 선택지가 없는 타입에서도 `null` 이 아니라 **빈 배열**입니다(서버 계약). 그래서
 * 화면에서 `options?.map` 같은 방어 코드를 쓰지 않습니다.
 */
export interface QuestionResponse {
  id: number
  type: QuestionType
  title: string
  required: boolean
  position: number
  minValue: number | null
  maxValue: number | null
  options: OptionResponse[]
}

/** 목록 항목입니다. 질문을 생략하는 대신 목록 화면이 실제로 쓰는 `responseCount` 를 담습니다. */
export interface FormSummaryResponse {
  id: number
  title: string
  status: FormStatus
  responseCount: number
  createdAt: string
}

/** 폼 상세입니다(생성·조회·수정 공용). */
export interface FormDetailResponse {
  id: number
  title: string
  description: string | null
  status: FormStatus
  slug: string
  questions: QuestionResponse[]
  createdAt: string
  updatedAt: string
}

/** 상태 전이 응답입니다. 상세 전체가 아니라 바뀐 결과만 담습니다. */
export interface FormStatusResponse {
  id: number
  status: FormStatus
  slug: string
  updatedAt: string
}

/** 질문 추가·수정 공용 요청입니다(수정은 선택지까지 전량 교체). */
export interface QuestionRequest {
  type: QuestionType
  title: string
  required: boolean
  position: number
  minValue: number | null
  maxValue: number | null
  options: { label: string; position: number }[]
}
