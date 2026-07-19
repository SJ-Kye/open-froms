import { apiClient } from '../../lib/apiClient'
import type { AnswerRequest, PublicFormResponse, SubmitResponseResult } from '../../types/api'

/**
 * 공개(익명) 경로 호출입니다.
 *
 * <p>인증이 필요 없는 경로지만 `apiClient` 를 그대로 씁니다. 제작자가 자기 폼 링크를 열면 토큰이
 * 함께 붙는데, 서버가 `permitAll` 이라 무시되고 응답도 달라지지 않습니다. 별도 인스턴스를 두면
 * baseURL·에러 포맷 처리가 두 벌이 됩니다.
 */

export async function fetchPublicForm(slug: string): Promise<PublicFormResponse> {
  const { data } = await apiClient.get<PublicFormResponse>(`/public/forms/${slug}`)
  return data
}

export async function submitResponse(
  slug: string,
  answers: AnswerRequest[],
): Promise<SubmitResponseResult> {
  const { data } = await apiClient.post<SubmitResponseResult>(
    `/public/forms/${slug}/responses`,
    { answers },
  )
  return data
}
