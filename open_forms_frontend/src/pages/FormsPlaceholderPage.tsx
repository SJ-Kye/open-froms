import { FilePlus2 } from 'lucide-react'
import EmptyState from '../components/EmptyState'

/** 폼 목록 자리표시입니다. 실제 목록·빌더는 다음 Phase 에서 이 자리에 들어옵니다. */
export default function FormsPlaceholderPage() {
  return (
    <>
      <h1>내 폼</h1>
      <div className="card" style={{ marginTop: 24 }}>
        <EmptyState
          icon={<FilePlus2 size={40} />}
          title="아직 만든 폼이 없습니다"
          description="폼 목록과 빌더는 다음 단계에서 이 자리에 들어옵니다."
        />
      </div>
    </>
  )
}
