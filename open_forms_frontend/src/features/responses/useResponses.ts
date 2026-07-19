import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { formKeys } from '../forms/useForms'
import * as responsesApi from './responsesApi'

export const responseKeys = {
  all: (formId: number) => ['forms', formId, 'responses'] as const,
  list: (formId: number, page: number) => ['forms', formId, 'responses', 'list', page] as const,
  detail: (formId: number, responseId: number) =>
    ['forms', formId, 'responses', 'detail', responseId] as const,
  summary: (formId: number) => ['forms', formId, 'summary'] as const,
}

export function useResponsesQuery(formId: number, page: number, size: number) {
  return useQuery({
    queryKey: responseKeys.list(formId, page),
    queryFn: () => responsesApi.fetchResponses(formId, page, size),
  })
}

export function useResponseQuery(formId: number, responseId: number) {
  return useQuery({
    queryKey: responseKeys.detail(formId, responseId),
    queryFn: () => responsesApi.fetchResponse(formId, responseId),
  })
}

export function useSummaryQuery(formId: number) {
  return useQuery({
    queryKey: responseKeys.summary(formId),
    queryFn: () => responsesApi.fetchSummary(formId),
  })
}

/**
 * 응답을 삭제합니다.
 *
 * <p>목록뿐 아니라 **집계와 폼 목록까지** 무효화합니다. 응답이 하나 줄면 총 응답 수·완료율·일별
 * 추이가 모두 달라지고, 폼 목록 카드의 `responseCount` 도 옛 값이 됩니다.
 */
export function useDeleteResponseMutation(formId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (responseId: number) => responsesApi.deleteResponse(formId, responseId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: responseKeys.all(formId) })
      void queryClient.invalidateQueries({ queryKey: responseKeys.summary(formId) })
      void queryClient.invalidateQueries({ queryKey: formKeys.all })
    },
  })
}
