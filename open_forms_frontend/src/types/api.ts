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

/** 설문지 생명주기입니다. 전이는 `DRAFT → PUBLISHED → CLOSED` 단방향이며 되돌릴 수 없습니다. */
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

/** 설문지 상세입니다(생성·조회·수정 공용). */
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

// ── 공개(응답자용) ──────────────────────────────────────────────────────────
// 제작자용과 별도 DTO 입니다. 설문지 id·질문 position·감사 컬럼이 없고, 대신 status 가 있습니다.

export interface PublicOptionResponse {
  id: number
  label: string
}

/** 배열 순서가 곧 표시 순서입니다(서버가 position 을 보내지 않습니다). */
export interface PublicQuestionResponse {
  id: number
  type: QuestionType
  title: string
  required: boolean
  minValue: number | null
  maxValue: number | null
  options: PublicOptionResponse[]
}

/**
 * 공개 링크로 조회한 설문지입니다.
 *
 * `status` 가 있는 이유는 **종료된 설문지도 200 으로 열리기** 때문입니다. 화면이 이 값을 보고 입력 화면과
 * 마감 안내를 갈라야 합니다. 미발행(`DRAFT`)은 애초에 404 라 여기로 오지 않습니다.
 */
export interface PublicFormResponse {
  slug: string
  title: string
  description: string | null
  status: FormStatus
  questions: PublicQuestionResponse[]
}

/**
 * 질문 하나에 대한 응답입니다. 질문 타입에 맞는 필드 **하나만** 채웁니다.
 * 답하지 않은 선택 질문은 항목 자체를 생략합니다.
 */
export interface AnswerRequest {
  questionId: number
  selectedOptionIds?: number[]
  textValue?: string
  numberValue?: number
  /** `YYYY-MM-DD` */
  dateValue?: string
  /** `HH:mm` */
  timeValue?: string
}

export interface SubmitResponseRequest {
  answers: AnswerRequest[]
}

/** 제출 결과입니다. 익명이라 제출 내용을 되돌려주지 않고 접수 사실만 확인해 줍니다. */
export interface SubmitResponseResult {
  responseId: number
  submittedAt: string
}

// ── 응답 조회(제작자용) ─────────────────────────────────────────────────────

/** 목록 항목입니다. `answeredCount` 는 답한 **질문 수**라 체크박스로 3개를 골라도 1입니다. */
export interface ResponseSummaryItem {
  responseId: number
  submittedAt: string
  answeredCount: number
  totalQuestions: number
}

export interface SelectedOption {
  optionId: number
  label: string
}

/**
 * 응답 상세의 문항별 답변입니다. 서버가 **설문지의 모든 질문**을 순서대로 담고, 답하지 않은 문항도
 * `answered: false` 로 포함합니다. 체크박스의 여러 저장 행은 여기서 하나로 묶여 옵니다.
 */
export interface AnswerDetail {
  questionId: number
  type: QuestionType
  title: string
  required: boolean
  answered: boolean
  selectedOptions: SelectedOption[]
  textValue: string | null
  numberValue: number | null
  dateValue: string | null
  timeValue: string | null
}

export interface ResponseDetailResponse {
  responseId: number
  submittedAt: string
  answers: AnswerDetail[]
}

// ── 집계 ───────────────────────────────────────────────────────────────────

export interface DailyCount {
  /** `YYYY-MM-DD` */
  date: string
  count: number
}

export interface OptionCount {
  optionId: number
  label: string
  count: number
}

export interface ValueCount {
  value: number
  count: number
}

/**
 * 질문 하나의 응답 분포입니다. 타입마다 채워지는 필드가 다르지만 **목록형 필드는 해당 없을 때도
 * 빈 배열**이라(서버 계약) 화면이 방어 코드 없이 순회할 수 있습니다.
 *
 * `optionCounts` 에는 아무도 고르지 않은 선택지도 `count: 0` 으로 들어 있습니다 — 빼 버리면 차트에서
 * 선택지가 통째로 사라져 마치 없었던 것처럼 보입니다.
 */
export interface QuestionStats {
  questionId: number
  type: QuestionType
  title: string
  required: boolean
  answeredCount: number
  average: number | null
  optionCounts: OptionCount[]
  valueCounts: ValueCount[]
  recentTexts: string[]
}

/**
 * 대시보드 집계입니다. 카드·추이·문항별 차트에 필요한 데이터를 한 번의 요청으로 모두 받습니다.
 *
 * `completionRate` 는 **0.0~1.0 비율**입니다(백분율이 아닙니다). `responsesByDate` 는 발행일부터
 * 종료일(미종료면 오늘)까지 빈 날을 0 으로 채워 이어지므로, 화면에서 날짜를 메울 필요가 없습니다.
 */
export interface FormSummaryStats {
  formId: number
  totalResponses: number
  completionRate: number
  responsesByDate: DailyCount[]
  questionSummaries: QuestionStats[]
}
