import type { QuestionType } from '../../types/api'

/**
 * 질문 유형의 화면 표현과 **타입별 규칙**을 한곳에 둡니다.
 *
 * <p>`isChoice`·`hasRange` 는 서버 `QuestionType.isChoice()`/`hasRange()` 와 같은 분류입니다. 같은
 * 규칙이 양쪽에 존재하는 것은 중복이지만, 없으면 빌더가 선택형에 선택지 입력칸을 언제 보여 줄지
 * 알 수 없어 사용자가 저장 버튼을 눌러야만 400 으로 배우게 됩니다. 대신 **검증의 최종 권한은
 * 서버**이고 화면은 안내만 담당한다는 경계를 지킵니다.
 */
export interface QuestionTypeMeta {
  value: QuestionType
  label: string
  /** 선택지를 갖는 타입인지 여부입니다(선택지 2개 이상 필수). */
  isChoice: boolean
  /** minValue~maxValue 범위 메타를 쓰는 타입인지 여부입니다. */
  hasRange: boolean
  /** 응답자가 보게 될 입력 형태를 한 줄로 설명합니다. */
  hint: string
}

export const QUESTION_TYPES: QuestionTypeMeta[] = [
  { value: 'SHORT_TEXT', label: '단답형', isChoice: false, hasRange: false, hint: '한 줄 텍스트' },
  { value: 'LONG_TEXT', label: '장문형', isChoice: false, hasRange: false, hint: '여러 줄 텍스트' },
  { value: 'SINGLE_CHOICE', label: '객관식(택1)', isChoice: true, hasRange: false, hint: '라디오 버튼' },
  { value: 'DROPDOWN', label: '드롭다운', isChoice: true, hasRange: false, hint: '목록에서 하나 선택' },
  { value: 'MULTIPLE_CHOICE', label: '체크박스(택N)', isChoice: true, hasRange: false, hint: '여러 개 선택 가능' },
  { value: 'RATING', label: '평점', isChoice: false, hasRange: true, hint: '척도 버튼(예: 1~5)' },
  { value: 'NUMBER', label: '숫자', isChoice: false, hasRange: true, hint: '숫자 입력' },
  { value: 'DATE', label: '날짜', isChoice: false, hasRange: false, hint: '날짜 선택' },
  { value: 'TIME', label: '시각', isChoice: false, hasRange: false, hint: '시각 선택' },
]

const BY_VALUE = new Map(QUESTION_TYPES.map((meta) => [meta.value, meta]))

export function questionTypeMeta(type: QuestionType): QuestionTypeMeta {
  const meta = BY_VALUE.get(type)
  if (!meta) {
    throw new Error(`알 수 없는 질문 유형입니다: ${type}`)
  }
  return meta
}

/** 선택형 질문에 필요한 최소 선택지 수입니다(서버 `OPTIONS_REQUIRED` 와 같은 값). */
export const MIN_OPTIONS = 2
