import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  fetchChildPendingTasks,
  fetchChildStudyRecord,
  fetchParentChildren,
} from '../../api/parentApi'
import DashboardCard from '../../components/DashboardCard'
import {
  GAUGE_CLASS,
  PARENT_TYPE_LABEL,
  nameInitial,
} from '../../components/parent/parentUtils'
import { subjectLabel } from '../../auth/subjectLabels'
import '../../styles/class-list.css'
import '../../styles/parent.css'

export default function ParentChildHomePage() {
  const { studentId } = useParams()
  const sid = Number(studentId)
  const [child, setChild] = useState(null)
  const [pending, setPending] = useState([])
  const [gauges, setGauges] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [children, tasks] = await Promise.all([
        fetchParentChildren(),
        fetchChildPendingTasks(sid),
      ])
      const found = children.find((c) => c.studentId === sid)
      if (!found) {
        setError('자녀 정보를 찾을 수 없습니다.')
        return
      }
      setChild(found)
      setPending(tasks)

      const gaugeMap = {}
      await Promise.all(
        (found.classes || []).map(async (c) => {
          try {
            gaugeMap[c.classId] = await fetchChildStudyRecord(sid, c.classId)
          } catch {
            /* 개별 반 조회 실패 시 게이지 생략 */
          }
        }),
      )
      setGauges(gaugeMap)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [sid])

  useEffect(() => {
    load()
  }, [load])

  if (loading) {
    return (
      <div className="ams-parent">
        <p className="ams-class-list-page__empty">불러오는 중…</p>
      </div>
    )
  }
  if (error || !child) {
    return (
      <div className="ams-parent">
        <p className="ams-class-list-page__error">{error || '오류'}</p>
      </div>
    )
  }

  return (
    <div className="ams-parent">
      <Link to="/parent" className="ams-parent__back">
        ← 자녀 목록
      </Link>

      <header className="ams-parent-profile">
        <span className="ams-parent-profile__avatar" aria-hidden>
          {nameInitial(child.studentName)}
        </span>
        <div>
          <h2 className="ams-parent-profile__name">{child.studentName}</h2>
          <p className="ams-parent-profile__hint">읽기 전용 · 학부모 조회</p>
        </div>
      </header>

      <div className="ams-parent-actions">
        <Link
          to={`/parent/children/${sid}/reports`}
          className="ams-parent-action ams-parent-action--primary"
        >
          <span className="ams-parent-action__label">성실도 보고서</span>
          <span className="ams-parent-action__desc">기간별 PDF 보고서 다운로드</span>
        </Link>
      </div>

      {pending.length > 0 && (
        <DashboardCard title={`미완료 ${pending.length}건`}>
          <ul className="ams-parent-pending">
            {pending.map((t) => (
              <li key={`${t.type}-${t.entityId}`} className="ams-parent-pending__item">
                <span className="ams-pill ams-pill--warning">
                  {PARENT_TYPE_LABEL[t.type] || t.type}
                </span>
                <strong>{t.title}</strong>
                <span className="ams-parent-pending__class">{t.className}</span>
              </li>
            ))}
          </ul>
        </DashboardCard>
      )}

      <p className="ams-parent__section-title">수강 반 · 공부기록</p>

      {!child.classes?.length ? (
        <div className="ams-parent-empty-card">
          <p className="ams-parent-empty-card__title">배정된 반이 없습니다</p>
          <p className="ams-parent-empty-card__desc">학원에서 수강반 배정 후 표시됩니다.</p>
        </div>
      ) : (
        <div className="ams-class-list">
          {child.classes.map((c) => {
            const record = gauges[c.classId]
            const gaugeTone = record ? GAUGE_CLASS[record.gaugeLevel] : null
            return (
              <Link
                key={c.classId}
                to={`/parent/children/${sid}/classes/${c.classId}`}
                className="ams-class-list__item"
              >
                <div className="ams-class-list__row">
                  <strong className="ams-class-list__name">{c.name}</strong>
                  {record ? (
                    <span
                      className={`ams-gauge ams-gauge--${gaugeTone}`}
                      title={record.encouragementMessage}
                    >
                      종합 {record.overallPercent}%
                    </span>
                  ) : (
                    <span className="ams-class-list__badge">{subjectLabel(c.subject)}</span>
                  )}
                </div>
                <span className="ams-class-list__meta">
                  {subjectLabel(c.subject)} · 공부기록 보기 →
                </span>
              </Link>
            )
          })}
        </div>
      )}
    </div>
  )
}
