import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { FormStatus, QuestionRequest } from '../../types/api'
import * as formsApi from './formsApi'

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

/**
 * 두 문항의 순서를 맞바꿉니다.
 *
 * <p>순서 전용 API 는 없고 `PUT` 이 문항 전체를 교체하므로, **position 만 바꾼 전체 페이로드**를 두
 * 번 보냅니다. `questions` 에 (form_id, position) 유니크 제약이 없어(V1__init.sql) 두 요청 사이에
 * 값이 잠시 겹쳐도 무방합니다. 순차로 보내는 이유는 동시에 보내면 실패 시 어느 쪽이 적용됐는지
 * 알기 어렵기 때문입니다.
 */
export function useReorderQuestionsMutation(formId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (swap: {
      first: { id: number; input: QuestionRequest }
      second: { id: number; input: QuestionRequest }
    }) => {
      await formsApi.updateQuestion(formId, swap.first.id, swap.first.input)
      await formsApi.updateQuestion(formId, swap.second.id, swap.second.input)
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: formKeys.detail(formId) }),
  })
}

export function useDeleteQuestionMutation(formId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (questionId: number) => formsApi.deleteQuestion(formId, questionId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: formKeys.detail(formId) }),
  })
}
