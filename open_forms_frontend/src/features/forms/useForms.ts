import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { FormStatus, QuestionRequest } from '../../types/api'
import * as formsApi from './formsApi'
import type { SavePlan } from './savePlan'

/**
 * 설문지 관련 서버 상태 훅입니다. 쿼리 키를 한곳에서 만들어, 무효화 대상이 흩어지지 않게 합니다.
 *
 * <p>질문 API 는 질문 단위(POST/PUT/DELETE)라 매번 상세를 다시 받아야 화면과 서버가 일치합니다.
 * 그래서 질문 변경 뮤테이션은 모두 해당 설문지 상세를 무효화합니다.
 */

export const formKeys = {
  all: ['forms'] as const,
  list: (page: number, status?: FormStatus) => ['forms', 'list', page, status ?? 'ALL'] as const,
  detail: (id: number) => ['forms', 'detail', id] as const,
}

export function useFormsQuery(page: number, size: number, status?: FormStatus) {
  return useQuery({
    queryKey: formKeys.list(page, status),
    queryFn: () => formsApi.fetchForms({ page, size, status }),
  })
}

export function useFormQuery(id: number) {
  return useQuery({
    queryKey: formKeys.detail(id),
    queryFn: () => formsApi.fetchForm(id),
  })
}

export function useCreateFormMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: formsApi.createForm,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: formKeys.all }),
  })
}

export function useUpdateFormMutation(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { title: string; description: string }) => formsApi.updateForm(id, input),
    // 목록에도 제목이 보이므로 상세만 갱신하면 목록이 옛 제목을 들고 있습니다.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: formKeys.all }),
  })
}

export function useChangeStatusMutation(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (status: FormStatus) => formsApi.changeFormStatus(id, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: formKeys.all }),
  })
}

export function useDeleteFormMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: formsApi.deleteForm,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: formKeys.all }),
  })
}

export function useCreateQuestionMutation(formId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: QuestionRequest) => formsApi.createQuestion(formId, input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: formKeys.detail(formId) }),
  })
}

export function useUpdateQuestionMutation(formId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ questionId, input }: { questionId: number; input: QuestionRequest }) =>
      formsApi.updateQuestion(formId, questionId, input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: formKeys.detail(formId) }),
  })
}

/** 일괄 저장의 결과입니다. 실패한 항목은 키로 되짚어 해당 카드에 사유를 붙입니다. */
export interface SaveOutcome {
  succeeded: number
  failures: Array<{ key: string; error: unknown }>
}

/**
 * 빌더의 변경을 **한 번의 저장**으로 반영합니다.
 *
 * <p>서버 API 가 문항 단위라 호출은 여러 번 나가지만, 사용자에게는 저장 버튼 하나입니다. 순서에
 * 이유가 있습니다 — 삭제를 먼저 해야 남은 문항에 부여할 position 이 빈틈 없이 맞고, 생성이 마지막인
 * 것은 새 문항이 목록의 끝자리를 차지하기 때문입니다.
 *
 * <p>하나가 실패해도 **멈추지 않습니다.** 각 호출은 서로 독립적이라, 중간에 중단하면 아무 문제 없던
 * 나머지 편집분까지 버려집니다. 대신 실패를 모아 돌려주고 화면이 해당 카드에 사유를 붙입니다.
 * (대부분의 400 은 보내기 전 draftProblem 검증에서 이미 걸러집니다.)
 */
export function useSaveBuilderMutation(formId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (plan: SavePlan): Promise<SaveOutcome> => {
      const failures: SaveOutcome['failures'] = []
      let succeeded = 0

      async function run(key: string, call: () => Promise<unknown>) {
        try {
          await call()
          succeeded += 1
        } catch (error) {
          failures.push({ key, error })
        }
      }

      for (const questionId of plan.deletes) {
        await run(`delete-${questionId}`, () => formsApi.deleteQuestion(formId, questionId))
      }
      if (plan.detailsChanged) {
        await run('details', () =>
          formsApi.updateForm(formId, { title: plan.title, description: plan.description }),
        )
      }
      for (const update of plan.updates) {
        await run(update.key, () =>
          formsApi.updateQuestion(formId, update.questionId, update.input),
        )
      }
      for (const create of plan.creates) {
        await run(create.key, () => formsApi.createQuestion(formId, create.input))
      }

      return { succeeded, failures }
    },
    // 목록 카드에도 제목·문항 수가 보이므로 상세만 갱신하면 목록이 옛 값을 들고 있습니다.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: formKeys.all }),
  })
}

export function useDeleteQuestionMutation(formId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (questionId: number) => formsApi.deleteQuestion(formId, questionId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: formKeys.detail(formId) }),
  })
}
