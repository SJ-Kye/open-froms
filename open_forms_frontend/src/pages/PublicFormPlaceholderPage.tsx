import { useParams } from 'react-router-dom'
import { ClipboardList } from 'lucide-react'
import EmptyState from '../components/EmptyState'

/**
 * 공개 응답 페이지 자리표시입니다. 인증 없이 열리는 경로임을 지금 확정해 두고(라우팅이 보호
 * 라우트 밖에 있음), 실제 응답 화면은 다음 Phase 에서 채웁니다.
 */
export default function PublicFormPlaceholderPage() {
  const { slug } = useParams()

  return (
    <div className="main-wrapper">
      <EmptyState
        icon={<ClipboardList size={40} />}
        title="공개 응답 페이지"
        description={`slug: ${slug} — 응답 화면은 다음 단계에서 이 자리에 들어옵니다.`}
      />
    </div>
  )
}
