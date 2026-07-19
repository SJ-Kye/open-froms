import { apiClient } from '../../lib/apiClient'
import type {
  FormDetailResponse,
  FormStatus,
  FormStatusResponse,
  FormSummaryResponse,
  PageResponse,
  QuestionRequest,
  QuestionResponse,
} from '../../types/api'

/** 폼·질문 API 호출을 한곳에 모읍니다. 경로와 본문 모양을 화면이 알 필요는 없습니다. */

export interface FormListParams {
  page: number
  size: number
  status?: FormStatus
}

export async function fetchForms(params: FormListParams): Promise<PageResponse<FormSummaryResponse>> {
  const { data } = await apiClient.get<PageResponse<FormSummaryResponse>>('/forms', {
    // status 가 없으면 전체 조회입니다. 빈 문자열을 보내면 서버가 값으로 받아 역직렬화에 실패하므로
    // 파라미터 자체를 빼야 합니다.
    params: { page: params.page, size: params.size, sort: 'createdAt,desc', ...(params.status ? { status: params.status } : {}) },
  })
  return data
}

export async function fetchForm(id: number): Promise<FormDetailResponse> {
  const { data } = await apiClient.get<FormDetailResponse>(`/forms/${id}`)
  return data
}

export async function createForm(input: {
  title: string
  description: string
}): Promise<FormDetailResponse> {
  const { data } = await apiClient.post<FormDetailResponse>('/forms', input)
  return data
}

export async function updateForm(
  id: number,
  input: { title: string; description: string },
): Promise<FormDetailResponse> {
  const { data } = await apiClient.put<FormDetailResponse>(`/forms/${id}`, input)
  return data
}

/** 상태 전이입니다. 되돌리는 전이·건너뛰는 전이는 서버가 409 로 거부합니다. */
export async function changeFormStatus(id: number, status: FormStatus): Promise<FormStatusResponse> {
  const { data } = await apiClient.patch<FormStatusResponse>(`/forms/${id}/status`, { status })
  return data
}

export async function deleteForm(id: number): Promise<void> {
  await apiClient.delete(`/forms/${id}`)
}

export async function createQuestion(
  formId: number,
  input: QuestionRequest,
): Promise<QuestionResponse> {
  const { data } = await apiClient.post<QuestionResponse>(`/forms/${formId}/questions`, input)
  return data
}

/** 질문 수정은 선택지까지 전량 교체입니다(서버 계약). 부분 수정 API 는 없습니다. */
export async function updateQuestion(
  formId: number,
  questionId: number,
  input: QuestionRequest,
): Promise<QuestionResponse> {
  const { data } = await apiClient.put<QuestionResponse>(
    `/forms/${formId}/questions/${questionId}`,
    input,
  )
  return data
}

export async function deleteQuestion(formId: number, questionId: number): Promise<void> {
  await apiClient.delete(`/forms/${formId}/questions/${questionId}`)
}
