import { apiClient } from '../../lib/apiClient'
import type {
  FormSummaryStats,
  PageResponse,
  ResponseDetailResponse,
  ResponseSummaryItem,
} from '../../types/api'

/** 수집된 응답과 집계를 읽는 경로입니다. 전부 인증·소유권 뒤에 있습니다(공개 경로에는 없습니다). */

export async function fetchResponses(
  formId: number,
  page: number,
  size: number,
): Promise<PageResponse<ResponseSummaryItem>> {
  const { data } = await apiClient.get<PageResponse<ResponseSummaryItem>>(
    `/forms/${formId}/responses`,
    { params: { page, size, sort: 'createdAt,desc' } },
  )
  return data
}

export async function fetchResponse(
  formId: number,
  responseId: number,
): Promise<ResponseDetailResponse> {
  const { data } = await apiClient.get<ResponseDetailResponse>(
    `/forms/${formId}/responses/${responseId}`,
  )
  return data
}

export async function deleteResponse(formId: number, responseId: number): Promise<void> {
  await apiClient.delete(`/forms/${formId}/responses/${responseId}`)
}

export async function fetchSummary(formId: number): Promise<FormSummaryStats> {
  const { data } = await apiClient.get<FormSummaryStats>(`/forms/${formId}/summary`)
  return data
}
