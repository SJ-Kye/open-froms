import { useEffect, useRef } from 'react'
import Spinner from './Spinner'
import styles from './ConfirmDialog.module.css'

/**
 * 되돌릴 수 없는 동작을 확인받습니다.
 *
 * <p>`window.confirm` 대신 두는 이유는 이 서비스에서 확인이 필요한 동작(설문지 삭제·발행·종료)이
 * 모두 **왜 되돌릴 수 없는지**를 함께 설명해야 하는 것들이기 때문입니다. 기본 confirm 은 서식을
 * 담을 수 없고 브라우저마다 모양이 다릅니다.
 *
 * <p>{@code <dialog>} 를 쓰므로 포커스 가둠·Esc 닫기·배경 비활성화를 브라우저가 처리합니다.
 */
export default function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  danger = false,
  pending = false,
  onConfirm,
  onCancel,
}: {
  open: boolean
  title: string
  description: string
  confirmLabel: string
  danger?: boolean
  pending?: boolean
  onConfirm: () => void
  onCancel: () => void
}) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) {
      return
    }
    if (open && !dialog.open) {
      dialog.showModal()
    } else if (!open && dialog.open) {
      dialog.close()
    }
  }, [open])

  return (
    <dialog
      ref={ref}
      className={styles.dialog}
      // Esc 로 닫아도 부모 상태가 열림으로 남지 않도록 취소로 취급합니다.
      onClose={onCancel}
      onCancel={(event) => {
        event.preventDefault()
        if (!pending) {
          onCancel()
        }
      }}
    >
      <h2 className={styles.title}>{title}</h2>
      <p className={styles.description}>{description}</p>
      <div className={styles.actions}>
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={pending}>
          취소
        </button>
        <button
          type="button"
          className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`}
          onClick={onConfirm}
          disabled={pending}
        >
          {pending && <Spinner size={14} />}
          {confirmLabel}
        </button>
      </div>
    </dialog>
  )
}
