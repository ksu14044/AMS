import { useEffect, useRef, useState } from 'react'
import {
  downloadReportImage,
  fetchReportDetail,
  updateReportComment,
} from '../api/reportsApi'
import { refreshReportImage } from '../utils/reportImageCapture'
import ReportDetailContent from './ReportDetailContent'

export default function ReportDetailModal({ reportId, canManage, onClose, onError }) {
  const [detail, setDetail] = useState(null)
  const [comment, setComment] = useState('')
  const [saving, setSaving] = useState(false)
  const [pngSyncing, setPngSyncing] = useState(false)
  const [loading, setLoading] = useState(true)
  const pngJobRef = useRef(0)

  useEffect(() => {
    ;(async () => {
      setLoading(true)
      onError('')
      try {
        const d = await fetchReportDetail(reportId)
        setDetail(d)
        setComment(d.teacherComment || '')
      } catch (err) {
        onError(err.message)
      } finally {
        setLoading(false)
      }
    })()
  }, [reportId, onError])

  async function handleSaveComment() {
    setSaving(true)
    onError('')
    let updated
    try {
      updated = await updateReportComment(reportId, comment)
      setDetail(updated)
      setComment(updated.teacherComment || '')
    } catch (err) {
      onError(err.message)
      return
    } finally {
      setSaving(false)
    }

    const job = ++pngJobRef.current
    setPngSyncing(true)
    try {
      await refreshReportImage(reportId, { ...updated, teacherComment: comment })
    } catch (err) {
      if (job === pngJobRef.current) {
        onError(err.message || 'PNG 반영에 실패했습니다.')
      }
    } finally {
      if (job === pngJobRef.current) {
        setPngSyncing(false)
      }
    }
  }

  async function handleDownloadImage() {
    onError('')
    try {
      await downloadReportImage(reportId)
    } catch (err) {
      onError(err.message)
    }
  }

  if (loading) {
    return (
      <div className="ams-report-modal">
        <p>불러오는 중…</p>
      </div>
    )
  }

  if (!detail) return null

  const displayDetail = { ...detail, teacherComment: canManage ? comment : detail.teacherComment }

  return (
    <div className="ams-report-modal-shell" role="dialog" aria-modal="true" aria-label="성실도 보고서">
      <header className="ams-report-modal__toolbar">
        <button type="button" className="ams-icon-btn" onClick={onClose} aria-label="닫기">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden
          >
            <path d="M15 5 8 12l7 7" />
          </svg>
        </button>
        <span className="ams-report-modal__toolbar-title">성실도 보고서</span>
        <button
          type="button"
          className="ams-icon-btn"
          onClick={handleDownloadImage}
          aria-label="PNG 다운로드"
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden
          >
            <path d="M12 4v11" />
            <path d="m7 10 5 5 5-5" />
            <path d="M5 20h14" />
          </svg>
        </button>
      </header>

      <ReportDetailContent
        detail={displayDetail}
        commentEdit={
          canManage
            ? {
                value: comment,
                onChange: setComment,
                onSave: handleSaveComment,
                saving,
                pngSyncing,
              }
            : undefined
        }
      />
    </div>
  )
}
