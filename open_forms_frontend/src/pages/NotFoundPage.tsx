import { Link } from 'react-router-dom'
import { SearchX } from 'lucide-react'
import EmptyState from '../components/EmptyState'

export default function NotFoundPage() {
  return (
    <div className="main-wrapper">
      <EmptyState
        icon={<SearchX size={40} />}
        title="페이지를 찾을 수 없습니다"
        description="주소가 바뀌었거나 삭제된 페이지일 수 있습니다."
        action={
          <Link to="/" className="btn btn-primary">
            처음으로
          </Link>
        }
      />
    </div>
  )
}
