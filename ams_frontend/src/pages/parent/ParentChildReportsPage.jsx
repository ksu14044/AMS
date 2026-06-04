import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchChildReports, fetchParentChildren } from '../../api/parentApi'
import { formatReportPeriod } from '../../api/reportsApi'
import ReportDetailModal from '../../components/ReportDetailModal'
import '../../styles/class-list.css'
import '../../styles/class-detail.css'
import '../../styles/parent.css'

export default function ParentChildReportsPage() {
  const { studentId } = useParams()
  const sid = Number(studentId)
  const [studentName, setStudentName] = useState('')
  const [reports, setReports] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedId, setSelectedId] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [children, list] = await Promise.all([fetchParentChildren(), fetchChildReports(sid)])
      const child = children.find((c) => c.studentId === sid)
      setStudentName(child?.studentName || '')
      setReports(list)
    } catch (err) {
      setError(err.message)
      setReports([])
    } finally {
      setLoading(false)
    }
  }, [sid])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div className="ams-parent">
      <Link to={`/parent/children/${sid}`} className="ams-parent__back">
        ← {studentName || '자녀'} 홈
      </Link>

      <section className="ams-parent__hero">
        <h2 className="ams-parent__hero-title">성실도 보고서</h2>
        <p className="ams-parent__hero-desc">
          {studentName ? `${studentName} 학생의 ` : ''}
          학원에서 생성한 기간별 보고서입니다. 항목을 눌러 내용을 확인할 수 있습니다.
        </p>
      </section>

      {loading ? (
        <p className="ams-class-list-page__empty">불러오는 중…</p>
      ) : error ? (
        <p className="ams-class-list-page__error">{error}</p>
      ) : reports.length === 0 ? (
        <div className="ams-parent-empty-card">
          <p className="ams-parent-empty-card__title">보고서가 없습니다</p>
          <p className="ams-parent-empty-card__desc">
            담임 선생님이 기간별 보고서를 생성하면 이곳에 표시됩니다.
          </p>
        </div>
      ) : (
        <ul className="ams-parent-reports">
          {reports.map((r) => (
            <li key={r.reportId} className="ams-parent-report">
              <div className="ams-parent-report__body">
                <span className="ams-parent-report__title">
                  {r.periodLabel || r.testTitle || '성실도 보고서'}
                </span>
                <span className="ams-parent-report__meta">
                  {formatReportPeriod(r.periodStart, r.periodEnd)}
                  {r.totalScore != null ? ` · 종합 ${r.totalScore}점` : ''}
                </span>
                {r.overallGrade && (
                  <span className="ams-parent-report__grade">{r.overallGrade}</span>
                )}
              </div>
              <button
                type="button"
                className="ams-btn ams-btn--primary ams-btn--sm"
                onClick={() => setSelectedId(r.reportId)}
              >
                보기
              </button>
            </li>
          ))}
        </ul>
      )}

      {selectedId && (
        <div className="ams-report-modal-backdrop" onClick={() => setSelectedId(null)}>
          <div onClick={(e) => e.stopPropagation()}>
            <ReportDetailModal
              reportId={selectedId}
              canManage={false}
              onClose={() => setSelectedId(null)}
              onError={setError}
            />
          </div>
        </div>
      )}
    </div>
  )
}
