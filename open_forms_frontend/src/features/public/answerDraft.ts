import type { AnswerRequest, PublicQuestionResponse } from '../../types/api'
import { questionTypeMeta } from '../forms/questionType'

/**
 * 작성 중인 응답입니다. 질문 id → 값이며, 타입마다 값의 모양이 다릅니다.
 *
 * <ul>
 *   <li>선택형 → 고른 선택지 id 배열</li>
 *   <li>그 외 → 입력칸의 문자열(숫자·날짜·시각도 입력 중에는 문자열입니다)</li>
 * </ul>
 *
 * <p>숫자를 문자열로 들고 있는 이유는 빌더와 같습니다 — 입력칸을 비우는 중간 상태("")가 숫자
 * 타입에는 없어서, 숫자로 두면 지우는 순간 0 이나 NaN 이 됩니다.
 */
export type AnswerValue = { kind: 'options'; optionIds: number[] } | { kind: 'text'; value: string }

export type AnswerDraft = Record<number, AnswerValue>

/** 질문의 초기값입니다. 선택형은 빈 배열, 그 외는 빈 문자열에서 시작합니다. */
export function initialDraft(questions: PublicQuestionResponse[]): AnswerDraft {
  const draft: AnswerDraft = {}
  for (const question of questions) {
    draft[question.id] = questionTypeMeta(question.type).isChoice
      ? { kind: 'options', optionIds: [] }
      : { kind: 'text', value: '' }
  }
  return draft
}

/** 이 문항에 답했는지 여부입니다. 필수 검사와 "생략할 항목" 판정에 함께 씁니다. */
export function isAnswered(value: AnswerValue | undefined): boolean {
  if (!value) {
    return false
  }
  return value.kind === 'options' ? value.optionIds.length > 0 : value.value.trim() !== ''
}

/**
 * 작성 상태를 제출 본문으로 바꿉니다.
 *
 * <p>**답하지 않은 문항은 항목 자체를 생략합니다**(서버 계약). 빈 값으로 보내도 미응답으로 취급되긴
 * 하지만, 보내지 않는 편이 "무엇을 답했는가"가 요청 본문에 그대로 드러나 디버깅이 쉽습니다.
 */
export function toAnswerRequests(
  questions: PublicQuestionResponse[],
  draft: AnswerDraft,
): AnswerRequest[] {
  const answers: AnswerRequest[] = []

  for (const question of questions) {
    const value = draft[question.id]
    if (!isAnswered(value)) {
      continue
    }

    if (value.kind === 'options') {
      answers.push({ questionId: question.id, selectedOptionIds: value.optionIds })
      continue
    }

    const raw = value.value.trim()
    switch (question.type) {
      case 'RATING':
      case 'NUMBER':
        answers.push({ questionId: question.id, numberValue: Number(raw) })
        break
      case 'DATE':
        // input[type=date] 의 값이 이미 YYYY-MM-DD 라 서버의 LocalDate 와 형식이 같습니다.
        answers.push({ questionId: question.id, dateValue: raw })
        break
      case 'TIME':
        // input[type=time] 의 값도 HH:mm 그대로입니다(초가 붙지 않습니다).
        answers.push({ questionId: question.id, timeValue: raw })
        break
      default:
        answers.push({ questionId: question.id, textValue: raw })
    }
  }

  return answers
}

/**
 * 필수인데 답하지 않은 문항의 id 입니다.
 *
 * <p>서버도 같은 검사를 하지만(400 `REQUIRED_ANSWER_MISSING`), 제출을 눌러야만 알게 하는 대신
 * 미리 알려 주려는 **안내용**입니다. 최종 판정은 서버가 합니다.
 */
export function missingRequiredIds(
  questions: PublicQuestionResponse[],
  draft: AnswerDraft,
): number[] {
  return questions
    .filter((question) => question.required && !isAnswered(draft[question.id]))
    .map((question) => question.id)
}

/**
 * 서버가 문항을 지목한 `fieldErrors` 를 질문 id → 사유로 폅니다.
 *
 * <p>`field` 가 `questionId:1` 형태인 것은 질문이 동적으로 생성되어 **고정된 필드명이 없기**
 * 때문입니다. 요청 배열의 인덱스로도 지목할 수 없습니다 — 누락된 문항은 애초에 배열에 없습니다.
 */
export function toQuestionErrors(fieldErrors: Record<string, string>): Record<number, string> {
  const result: Record<number, string> = {}
  for (const [field, message] of Object.entries(fieldErrors)) {
    const match = /^questionId:(\d+)$/.exec(field)
    if (match) {
      result[Number(match[1])] = message
    }
  }
  return result
}
