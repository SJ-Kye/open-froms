import type { QuestionRequest, QuestionResponse, QuestionType } from '../../types/api'
import { MIN_OPTIONS, questionTypeMeta } from './questionType'

/**
 * 편집 중인 질문입니다. 서버 표현(`QuestionResponse`)과 다른 점이 두 가지 있습니다.
 *
 * <ul>
 *   <li>범위(min/max)를 **문자열로** 들고 있습니다. 입력칸을 비우는 중간 상태("")가 숫자 타입에는
 *       없어서, 숫자로 두면 지우는 순간 0 이 되거나 NaN 이 됩니다.</li>
 *   <li>선택지에 클라이언트 전용 키가 있습니다. 서버 id 는 새 선택지에 없고 label 은 중복될 수
 *       있어, 목록 렌더링의 key 로 쓸 안정적인 값이 필요합니다.</li>
 * </ul>
 */
export interface OptionDraft {
  key: string
  label: string
}

export interface QuestionDraft {
  type: QuestionType
  title: string
  required: boolean
  minValue: string
  maxValue: string
  options: OptionDraft[]
}

let optionKeySeq = 0

function newOption(label = ''): OptionDraft {
  optionKeySeq += 1
  return { key: `opt-${optionKeySeq}`, label }
}

/** 새 질문의 초기값입니다. 가장 흔한 단답형에서 시작합니다. */
export function emptyDraft(): QuestionDraft {
  return {
    type: 'SHORT_TEXT',
    title: '',
    required: false,
    minValue: '',
    maxValue: '',
    options: [newOption(), newOption()],
  }
}

/** 저장된 질문을 편집 상태로 되돌립니다. */
export function toDraft(question: QuestionResponse): QuestionDraft {
  const meta = questionTypeMeta(question.type)
  return {
    type: question.type,
    title: question.title,
    required: question.required,
    minValue: question.minValue?.toString() ?? '',
    maxValue: question.maxValue?.toString() ?? '',
    options: meta.isChoice
      ? question.options.map((option) => newOption(option.label))
      : [newOption(), newOption()],
  }
}

export function addOption(draft: QuestionDraft): QuestionDraft {
  return { ...draft, options: [...draft.options, newOption()] }
}

/**
 * 편집 상태를 요청 본문으로 바꿉니다.
 *
 * <p>타입에 해당하지 않는 필드는 아예 비워 보냅니다. 서버는 이를 무시하도록 되어 있어(문서 05)
 * 보내도 오류는 아니지만, 타입을 여러 번 바꾼 뒤 남은 값이 그대로 저장되면 나중에 타입을 되돌렸을
 * 때 유령 같은 옛 값이 되살아납니다.
 *
 * @param position 1-기반 순서입니다(서버 `@Positive`).
 */
export function toRequest(draft: QuestionDraft, position: number): QuestionRequest {
  const meta = questionTypeMeta(draft.type)
  return {
    type: draft.type,
    title: draft.title.trim(),
    required: draft.required,
    position,
    minValue: meta.hasRange ? toNullableInt(draft.minValue) : null,
    maxValue: meta.hasRange ? toNullableInt(draft.maxValue) : null,
    options: meta.isChoice
      ? draft.options
          .map((option, index) => ({ label: option.label.trim(), position: index + 1 }))
          // 빈 칸은 사용자가 아직 채우지 않은 자리이지 "빈 선택지"가 아닙니다. 그대로 보내면
          // @NotBlank 위반으로 400 이 됩니다.
          .filter((option) => option.label.length > 0)
      : [],
  }
}

function toNullableInt(value: string): number | null {
  const trimmed = value.trim()
  if (trimmed === '') {
    return null
  }
  const parsed = Number(trimmed)
  return Number.isInteger(parsed) ? parsed : null
}

/**
 * 저장을 눌러도 되는지 화면에서 미리 판단합니다. **서버 규칙의 사본이 아니라 안내용**이며, 최종
 * 판정은 언제나 서버가 합니다(여기서 통과해도 400 이 올 수 있습니다).
 *
 * @returns 막아야 할 이유. 저장 가능하면 null 입니다.
 */
export function draftProblem(draft: QuestionDraft): string | null {
  if (draft.title.trim() === '') {
    return '질문 내용을 입력해 주세요.'
  }
  const meta = questionTypeMeta(draft.type)
  if (meta.isChoice) {
    const filled = draft.options.filter((option) => option.label.trim() !== '')
    if (filled.length < MIN_OPTIONS) {
      return `선택지를 ${MIN_OPTIONS}개 이상 입력해 주세요.`
    }
  }
  if (meta.hasRange) {
    const min = toNullableInt(draft.minValue)
    const max = toNullableInt(draft.maxValue)
    // 한쪽만 있는 것은 허용됩니다(서버도 둘 다 있을 때만 대소를 검증합니다).
    if (min !== null && max !== null && min > max) {
      return '최솟값이 최댓값보다 클 수 없습니다.'
    }
  }
  return null
}
