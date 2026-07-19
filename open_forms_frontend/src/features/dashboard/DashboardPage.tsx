import { useParams } from 'react-router-dom'
import { BarChart3, ListChecks, TrendingUp } from 'lucide-react'
import EmptyState from '../../components/EmptyState'
import ErrorBanner from '../../components/ErrorBanner'
import Spinner from '../../components/Spinner'
import { toApiError } from '../../lib/apiError'
import { useFormQuery } from '../forms/useForms'
import { useSummaryQuery } from '../responses/useResponses'
import QuestionStatsCard from './QuestionStatsCard'
import ResponsesTrendChart from './ResponsesTrendChart'
import styles from './DashboardPage.module.css'

/**
 * 집계 대시보드입니다. 서버가 카드·추이·문항별 통계를 **한 번의 요청**으로 모두 내려 주므로
 * 화면이 폼 구조와 응답을 따로 조회해 맞출 일이 없습니다.
 */
export default function DashboardPage() {
  const { id } = useParams()
  const formId = Number(id)
  const { data: form } = useFormQuery(formId)
  const { data, isPending, isError, error } = useSummaryQuery(formId)

  if (isPending) {
    return <Spinner page />
  }
  if (isError) {
    const apiError = toApiError(error)
    return <ErrorBanner message={apiError.message} traceId={apiError.traceId} />
  }

  if (data.totalResponses === 0) {
    return (
      <div className="card">
        {/* 응답이 0이면 빈 차트를 그리지 않습니다. 축만 남은 차트는 정보가 아니라 소음입니다. */}
        <EmptyState
          icon={<BarChart3 size={40} />}
          title={form?.status === 'DRAFT' ? '발행 후 집계가 시작됩니다' : '아직 집계할 응답이 없습니다'}
          description={
            form?.status === 'DRAFT'
              ? '작성 중인 폼은 응답을 받을 수 없어 집계할 것이 없습니다.'
              : '공개 링크로 응답이 들어오면 이곳에 통계가 나타납니다.'
          }
        />
      </div>
    )
  }

  const questionCount = data.questionSummaries.length
  // completionRate 는 0.0~1.0 비율입니다(백분율이 아님 — 서버 계약).
  const completionPercent = Math.round(data.completionRate * 100)

  return (
    <div>
      <div className={styles.tiles}>
        <article className={`card ${styles.tile}`}>
          <p className={styles.tileLabel}>총 응답</p>
          <p className={styles.tileValue}>{data.totalResponses.toLocaleString()}</p>
        </article>

        <article className={`card ${styles.tile}`}>
          <p className={styles.tileLabel}>평균 완료율</p>
          <p className={styles.tileValue}>{completionPercent}%</p>
          <div className={styles.meterTrack}>
            <div className={styles.meterFill} style={{ width: `${completionPercent}%` }} />
          </div>
          {/* 이 값이 무엇의 비율인지 적어 둡니다. 안 적으면 "필수 문항 응답률"로 오해됩니다. */}
          <p className={styles.tileHint}>응답 1건당 답한 문항 수 ÷ 전체 문항 수의 평균입니다.</p>
        </article>

        <article className={`card ${styles.tile}`}>
          <p className={styles.tileLabel}>문항 수</p>
          <p className={styles.tileValue}>{questionCount}</p>
        </article>
      </div>

      <section className={`card ${styles.section}`}>
        <h2 className={styles.sectionTitle}>
          <TrendingUp size={17} />
          일별 응답 추이
        </h2>
        <p className={styles.sectionHint}>
          발행일부터 {form?.status === 'CLOSED' ? '마감일' : '오늘'}까지, 응답이 없던 날도 0 으로
          이어 그립니다.
        </p>
        {data.responsesByDate.length === 0 ? (
          <p className={styles.noData}>표시할 기간이 없습니다.</p>
        ) : (
          <ResponsesTrendChart data={data.responsesByDate} />
        )}
      </section>

      <h2 className={styles.sectionTitle} style={{ marginBottom: 16 }}>
        <ListChecks size={17} />
        문항별 응답
      </h2>
      <div className={styles.questionGrid}>
        {data.questionSummaries.map((stats) => (
          <QuestionStatsCard
            key={stats.questionId}
            stats={stats}
            totalResponses={data.totalResponses}
          />
        ))}
      </div>
    </div>
  )
}
