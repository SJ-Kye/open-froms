import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CalendarDays, ClipboardList, MessageSquare, Pencil, Plus, Trash2 } from 'lucide-react'
import ConfirmDialog from '../../components/ConfirmDialog'
import EmptyState from '../../components/EmptyState'
import ErrorBanner from '../../components/ErrorBanner'
import Spinner from '../../components/Spinner'
import { toApiError } from '../../lib/apiError'
import { formatDate } from '../../lib/formatDate'
import type { FormStatus, FormSummaryResponse } from '../../types/api'
import StatusBadge from './StatusBadge'
import { useCreateFormMutation, useDeleteFormMutation, useFormsQuery } from './useForms'
import styles from './FormsListPage.module.css'

const PAGE_SIZE = 12

const FILTERS: { value: FormStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: '전체' },
  { value: 'DRAFT', label: '작성 중' },
  { value: 'PUBLISHED', label: '공개 중' },
  { value: 'CLOSED', label: '종료됨' },
]

export default function FormsListPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [filter, setFilter] = useState<FormStatus | 'ALL'>('ALL')
  const [pendingDelete, setPendingDelete] = useState<FormSummaryResponse | null>(null)

  const status = filter === 'ALL' ? undefined : filter
  const { data, isPending, isError, error } = useFormsQuery(page, PAGE_SIZE, status)
  const createForm = useCreateFormMutation()
  const deleteForm = useDeleteFormMutation()

  function changeFilter(next: FormStatus | 'ALL') {
    setFilter(next)
    // 필터를 바꾸면 결과 수가 달라지므로 첫 페이지로 돌아갑니다. 3페이지를 보던 중 필터를 좁히면
    // 존재하지 않는 페이지가 되어 빈 화면이 나옵니다.
    setPage(0)
  }

  async function handleCreate() {
    // 제목을 먼저 묻지 않고 빈 폼을 만들어 빌더로 보냅니다. 폼 작성은 제목 하나로 끝나지 않으므로
    // 입력을 두 단계로 나누면 사용자가 같은 화면을 두 번 보게 됩니다.
    const created = await createForm.mutateAsync({ title: '제목 없는 폼', description: '' })
    navigate(`/forms/${created.id}`)
  }

  async function handleDelete() {
    if (!pendingDelete) {
      return
    }
    await deleteForm.mutateAsync(pendingDelete.id)
    setPendingDelete(null)
  }

  return (
    <div className="animate-fade-in">
      <div className={styles.header}>
        <div>
          <h1>내 폼</h1>
          <p className={styles.subtitle}>만든 폼을 발행하고 응답을 모아 보세요.</p>
        </div>
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => void handleCreate()}
          disabled={createForm.isPending}
        >
          {createForm.isPending ? <Spinner size={16} /> : <Plus size={18} />}
          새 폼 만들기
        </button>
      </div>

      {createForm.isError && (
        <div style={{ marginBottom: 16 }}>
          <ErrorBanner {...bannerProps(createForm.error)} />
        </div>
      )}
      {deleteForm.isError && (
        <div style={{ marginBottom: 16 }}>
          <ErrorBanner {...bannerProps(deleteForm.error)} />
        </div>
      )}

      <div className={styles.filters}>
        {FILTERS.map((item) => (
          <button
            key={item.value}
            type="button"
            className={`${styles.filter} ${filter === item.value ? styles.filterActive : ''}`}
            onClick={() => changeFilter(item.value)}
            aria-pressed={filter === item.value}
          >
            {item.label}
          </button>
        ))}
      </div>

      {isPending ? (
        <Spinner page />
      ) : isError ? (
        <ErrorBanner {...bannerProps(error)} />
      ) : data.content.length === 0 ? (
        <div className="card">
          {/*
            "폼이 하나도 없음"과 "필터에 걸리는 폼이 없음"은 다른 상황입니다. 뭉뚱그리면 폼을 여러 개
            만든 사용자가 필터를 좁혔을 때 "아직 만든 폼이 없습니다"를 보고 데이터가 사라졌다고
            오해합니다.
          */}
          {filter === 'ALL' ? (
            <EmptyState
              icon={<ClipboardList size={40} />}
              title="아직 만든 폼이 없습니다"
              description="첫 폼을 만들어 질문을 추가하고, 링크를 공유해 응답을 받아 보세요."
              action={
                <button type="button" className="btn btn-primary" onClick={() => void handleCreate()}>
                  <Plus size={18} />첫 폼 만들기
                </button>
              }
            />
          ) : (
            <EmptyState
              icon={<ClipboardList size={40} />}
              title="조건에 맞는 폼이 없습니다"
              description="다른 상태로 필터를 바꿔 보세요."
              action={
                <button type="button" className="btn btn-secondary" onClick={() => changeFilter('ALL')}>
                  전체 보기
                </button>
              }
            />
          )}
        </div>
      ) : (
        <>
          <div className={styles.grid}>
            {data.content.map((form) => (
              <article key={form.id} className={`card ${styles.card}`}>
                <div>
                  <StatusBadge status={form.status} />
                  <h2 className={styles.cardTitle} style={{ marginTop: 10 }}>
                    {form.title}
                  </h2>
                </div>

                <div className={styles.meta}>
                  <span className={styles.metaItem}>
                    <MessageSquare size={14} />
                    응답 {form.responseCount}개
                  </span>
                  <span className={styles.metaItem}>
                    <CalendarDays size={14} />
                    {formatDate(form.createdAt)}
                  </span>
                </div>

                <div className={styles.actions}>
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => navigate(`/forms/${form.id}`)}
                  >
                    <Pencil size={15} />
                    {/* DRAFT 가 아니면 질문을 고칠 수 없으므로(서버 409) 라벨로 미리 알립니다. */}
                    {form.status === 'DRAFT' ? '편집' : '열기'}
                  </button>
                  <button
                    type="button"
                    className="btn btn-danger"
                    onClick={() => setPendingDelete(form)}
                  >
                    <Trash2 size={15} />
                    삭제
                  </button>
                </div>
              </article>
            ))}
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
                {data.page + 1} / {data.totalPages} 페이지 · 전체 {data.totalElements}개
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
        </>
      )}

      <ConfirmDialog
        open={pendingDelete !== null}
        title="폼을 삭제할까요?"
        description={
          pendingDelete
            ? `"${pendingDelete.title}" 과(와) 여기에 달린 질문·응답 ${pendingDelete.responseCount}건이 함께 삭제됩니다. 되돌릴 수 없습니다.`
            : ''
        }
        confirmLabel="삭제"
        danger
        pending={deleteForm.isPending}
        onConfirm={() => void handleDelete()}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  )
}

function bannerProps(error: unknown) {
  const apiError = toApiError(error)
  return { message: apiError.message, traceId: apiError.traceId }
}
