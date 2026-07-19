import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Trash2 } from 'lucide-react'
import ConfirmDialog from '../../components/ConfirmDialog'
import ErrorBanner from '../../components/ErrorBanner'
import Spinner from '../../components/Spinner'
import { useToast } from '../../components/useToast'
import { toApiError } from '../../lib/apiError'
import { formatDateTime } from '../../lib/formatDate'
import type { AnswerDetail } from '../../types/api'
import { useDeleteResponseMutation, useResponseQuery } from './useResponses'
import styles from './ResponsesPage.module.css'

/**
 * 응답 1건의 상세입니다.
 *
 * <p>서버가 **설문지의 모든 질문**을 순서대로, 답하지 않은 문항까지 `answered: false` 로 담아 주므로
 * 화면은 설문지 구조를 따로 조회하지 않습니다. 체크박스의 여러 저장 행도 이미 한 문항으로 묶여 옵니다.
 */
export default function ResponseDetailPage() {
  const { id, responseId } = useParams()
  const formId = Number(id)
  const targetId = Number(responseId)
  const navigate = useNavigate()
  const showToast = useToast()
  const [confirming, setConfirming] = useState(false)

  const { data, isPending, isError, error } = useResponseQuery(formId, targetId)
  const deleteResponse = useDeleteResponseMutation(formId)

  if (isPending) {
    return <Spinner page />
  }
  if (isError) {
    const apiError = toApiError(error)
    return (
      <div>
        <BackLink formId={formId} />
        <ErrorBanner message={apiError.message} traceId={apiError.traceId} />
      </div>
    )
  }

  return (
    <div>
      <BackLink formId={formId} />

      <div className={styles.detailHead}>
        <h2 style={{ fontSize: '1.05rem' }}>{formatDateTime(data.submittedAt)} 제출</h2>
        <button type="button" className="btn btn-danger" onClick={() => setConfirming(true)}>
          <Trash2 size={15} />
          삭제
        </button>
      </div>

      {deleteResponse.isError && (
        <div style={{ marginBottom: 16 }}>
          <ErrorBanner {...toBanner(deleteResponse.error)} />
        </div>
      )}

      <div className={styles.answers}>
        {data.answers.map((answer, index) => (
          <article key={answer.questionId} className={`card ${styles.answer}`}>
            <p className={styles.answerTitle}>
              {index + 1}. {answer.title}
              {answer.required && <span style={{ color: 'var(--error)' }}> *</span>}
            </p>
            <AnswerValue answer={answer} />
          </article>
        ))}
      </div>

      <ConfirmDialog
        open={confirming}
        title="이 응답을 삭제할까요?"
        description="되돌릴 수 없으며 집계도 함께 다시 계산됩니다."
        confirmLabel="삭제"
        danger
        pending={deleteResponse.isPending}
        onConfirm={() => {
          // 삭제한 응답의 상세에 머무를 수 없으므로 목록으로 돌아갑니다.
          void deleteResponse.mutateAsync(targetId).then(() => {
            navigate(`/forms/${formId}/responses`, { replace: true })
            showToast('응답을 삭제했습니다.')
          })
        }}
        onCancel={() => setConfirming(false)}
      />
    </div>
  )
}

/** 타입별로 값이 담긴 필드가 다릅니다. `answered` 가 무응답 판정을 대신해 줍니다. */
function AnswerValue({ answer }: { answer: AnswerDetail }) {
  if (!answer.answered) {
    // 무응답을 빈칸으로 두면 "값이 비어 있다"와 구분되지 않습니다.
    return <p className={`${styles.answerValue} ${styles.unanswered}`}>무응답</p>
  }

  if (answer.selectedOptions.length > 0) {
    return (
      <div className={styles.chips}>
        {answer.selectedOptions.map((option) => (
          <span key={option.optionId} className={styles.chip}>
            {option.label}
          </span>
        ))}
      </div>
    )
  }

  const value =
    answer.textValue ??
    answer.numberValue?.toString() ??
    answer.dateValue ??
    answer.timeValue ??
    '—'

  return <p className={styles.answerValue}>{value}</p>
}

function BackLink({ formId }: { formId: number }) {
  return (
    <Link
      to={`/forms/${formId}/responses`}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 4,
        fontSize: '0.88rem',
        marginBottom: 16,
        color: 'var(--text-secondary)',
      }}
    >
      <ArrowLeft size={16} />
      응답 목록
    </Link>
  )
}

function toBanner(error: unknown) {
  const apiError = toApiError(error)
  return { message: apiError.message, traceId: apiError.traceId }
}
