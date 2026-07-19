import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Inbox, Trash2 } from 'lucide-react'
import ConfirmDialog from '../../components/ConfirmDialog'
import EmptyState from '../../components/EmptyState'
import ErrorBanner from '../../components/ErrorBanner'
import Spinner from '../../components/Spinner'
import { useToast } from '../../components/useToast'
import { toApiError } from '../../lib/apiError'
import { formatDateTime } from '../../lib/formatDate'
import type { ResponseSummaryItem } from '../../types/api'
import { useFormQuery } from '../forms/useForms'
import { useDeleteResponseMutation, useResponsesQuery } from './useResponses'
import styles from './ResponsesPage.module.css'

const PAGE_SIZE = 20

/** 수집된 응답 목록입니다. 답변 내용은 상세에서 보고, 여기서는 "언제·얼마나 답했는가"만 봅니다. */
export default function ResponsesPage() {
  const { id } = useParams()
  const formId = Number(id)
  const navigate = useNavigate()
  const showToast = useToast()
  const [page, setPage] = useState(0)
  const [pendingDelete, setPendingDelete] = useState<ResponseSummaryItem | null>(null)

  const { data: form } = useFormQuery(formId)
  const { data, isPending, isError, error } = useResponsesQuery(formId, page, PAGE_SIZE)
  const deleteResponse = useDeleteResponseMutation(formId)

  if (isPending) {
    return <Spinner page />
  }
  if (isError) {
    const apiError = toApiError(error)
    return <ErrorBanner message={apiError.message} traceId={apiError.traceId} />
  }

  if (data.content.length === 0) {
    return (
      <div className="card">
        {/*
          "아직 응답이 없음"과 "받을 수 없는 상태"는 다릅니다. 발행 전 폼은 링크 자체가 열리지
          않으므로 응답이 0인 것이 당연하고, 사용자가 할 일은 기다리기가 아니라 발행하기입니다.
        */}
        <EmptyState
          icon={<Inbox size={40} />}
          title={form?.status === 'DRAFT' ? '아직 발행하지 않았습니다' : '아직 응답이 없습니다'}
          description={
            form?.status === 'DRAFT'
              ? '발행하면 공개 링크로 응답을 받을 수 있습니다.'
              : '공개 링크를 공유하면 응답이 이곳에 쌓입니다.'
          }
        />
      </div>
    )
  }

  return (
    <div>
      {deleteResponse.isError && (
        <div style={{ marginBottom: 16 }}>
          <ErrorBanner {...toBanner(deleteResponse.error)} />
        </div>
      )}

      <div className={styles.list}>
        {data.content.map((item) => {
          const ratio = item.totalQuestions === 0 ? 0 : item.answeredCount / item.totalQuestions
          return (
            <article key={item.responseId} className={`card ${styles.row}`}>
              <span className={styles.when}>{formatDateTime(item.submittedAt)}</span>

              <div className={styles.progress}>
                <div className={styles.progressLabel}>
                  <span>응답률</span>
                  <span>
                    {item.answeredCount}/{item.totalQuestions} 문항
                  </span>
                </div>
                <div
                  className={styles.progressTrack}
                  role="img"
                  aria-label={`${item.totalQuestions}문항 중 ${item.answeredCount}문항 응답`}
                >
                  <div className={styles.progressFill} style={{ width: `${ratio * 100}%` }} />
                </div>
              </div>

              <div className={styles.rowActions}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => navigate(`/forms/${formId}/responses/${item.responseId}`)}
                >
                  자세히
                </button>
                <button
                  type="button"
                  className="btn btn-danger"
                  onClick={() => setPendingDelete(item)}
                  aria-label="응답 삭제"
                >
                  <Trash2 size={15} />
                </button>
              </div>
            </article>
          )
        })}
      </div>

      {data.totalPages > 1 && (
        <div className={styles.pagination}>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => setPage((current) => current - 1)}
            disabled={page === 0}
          >
            이전
          </button>
          <span className={styles.pageInfo}>
            {data.page + 1} / {data.totalPages} 페이지 · 전체 {data.totalElements}건
          </span>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => setPage((current) => current + 1)}
            disabled={data.page + 1 >= data.totalPages}
          >
            다음
          </button>
        </div>
      )}

      <ConfirmDialog
        open={pendingDelete !== null}
        title="이 응답을 삭제할까요?"
        description={
          pendingDelete
            ? `${formatDateTime(pendingDelete.submittedAt)} 에 제출된 응답이 삭제됩니다. 되돌릴 수 없으며 집계도 함께 다시 계산됩니다.`
            : ''
        }
        confirmLabel="삭제"
        danger
        pending={deleteResponse.isPending}
        onConfirm={() => {
          if (pendingDelete) {
            void deleteResponse.mutateAsync(pendingDelete.responseId).then(() => {
              setPendingDelete(null)
              showToast('응답을 삭제했습니다.')
            })
          }
        }}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  )
}

function toBanner(error: unknown) {
  const apiError = toApiError(error)
  return { message: apiError.message, traceId: apiError.traceId }
}
