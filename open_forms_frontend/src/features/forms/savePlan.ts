import type { FormDetailResponse, QuestionRequest, QuestionResponse } from '../../types/api'
import { toDraft, toRequest, type QuestionDraft } from './questionDraft'

/**
 * 빌더가 화면에 들고 있는 문항 하나입니다. 저장된 문항과 아직 저장하지 않은 문항을 **같은 모양**으로
 * 다루는 것이 요점입니다 — 그래야 순서 변경이 단순한 배열 조작이 되고, 삭제는 배열에서 빼는 일이
 * 됩니다.
 */
export interface BuilderItem {
  /** 렌더·오류 매칭용 안정 키입니다. 서버 id 가 없는 새 문항에도 필요합니다. */
  key: string
  /** null 이면 아직 서버에 없습니다. */
  questionId: number | null
  draft: QuestionDraft
}

export interface SavePlan {
  detailsChanged: boolean
  title: string
  description: string
  /** 화면에서 사라진 저장 문항입니다(서버에는 아직 있음). */
  deletes: number[]
  updates: Array<{ key: string; questionId: number; input: QuestionRequest }>
  creates: Array<{ key: string; input: QuestionRequest }>
  changeCount: number
}

export function toItem(question: QuestionResponse): BuilderItem {
  return { key: `q-${question.id}`, questionId: question.id, draft: toDraft(question) }
}

/**
 * 서버 상태와 화면 상태를 비교해 **무엇을 보낼지**를 값으로 계산합니다.
 *
 * <p>position 은 서버가 보관하던 값이 아니라 **화면에서의 자리(index + 1)** 로 매깁니다. 그래서
 * 순서 변경이 별도 API 없이도 성립합니다 — 저장 시 전 문항의 position 을 새로 부여하기 때문입니다.
 * `questions` 에 (form_id, position) 유니크 제약이 없어(V1__init.sql) 갱신 도중 값이 겹쳐도
 * 무방합니다.
 *
 * <p>비교는 **서버로 보낼 형태끼리** 합니다. 화면에만 있는 차이(선택지의 클라이언트 key, 공백만
 * 다른 제목, 타입에 해당 없는 잔여 값)를 변경으로 세면 «변경 N건»이 늘 0 이 아니게 됩니다.
 */
export function buildSavePlan(
  form: FormDetailResponse,
  title: string,
  description: string,
  items: BuilderItem[],
): SavePlan {
  const trimmedTitle = title.trim()
  const detailsChanged =
    trimmedTitle !== form.title || description !== (form.description ?? '')

  const alive = new Set(items.map((item) => item.questionId).filter((id) => id !== null))
  const deletes = form.questions.filter((q) => !alive.has(q.id)).map((q) => q.id)

  const updates: SavePlan['updates'] = []
  const creates: SavePlan['creates'] = []

  items.forEach((item, index) => {
    const input = toRequest(item.draft, index + 1)
    if (item.questionId === null) {
      creates.push({ key: item.key, input })
      return
    }
    const saved = form.questions.find((q) => q.id === item.questionId)
    // 서버에서 이미 사라진 문항(다른 창에서 삭제 등)은 되살리지 않고 그냥 넘깁니다.
    if (!saved) {
      return
    }
    if (JSON.stringify(input) !== JSON.stringify(toRequest(toDraft(saved), index + 1))) {
      updates.push({ key: item.key, questionId: item.questionId, input })
    }
  })

  return {
    detailsChanged,
    title: trimmedTitle,
    description,
    deletes,
    updates,
    creates,
    changeCount: (detailsChanged ? 1 : 0) + deletes.length + updates.length + creates.length,
  }
}
