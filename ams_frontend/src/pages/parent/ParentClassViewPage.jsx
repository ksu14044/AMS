import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchChildStudyRecord, fetchParentChildren } from '../../api/parentApi'
import { GAUGE_CLASS } from '../../components/parent/parentUtils'
import { subjectLabel } from '../../auth/subjectLabels'
import '../../styles/class-detail.css'
import '../../styles/class-list.css'
import '../../styles/parent.css'

function MetricRow({ label, metric }) {
  if (!metric || metric.totalCount === 0) {
    return (
      <li className="ams-study-metric">
        <span>{label}</span>
        <span>—</span>
      </li>
    )
  }
  const tone = metric.ratePercent >= 75 ? 'green' : metric.ratePercent >= 50 ? 'orange' : 'red'
  return (
    <li className="ams-study-metric">
      <span>{label}</span>
      <span className={`ams-gauge ams-gauge--${tone}`}>
        {metric.completedCount}/{metric.totalCount} ({metric.ratePercent}%)
      </span>
    </li>
  )
}

export default function ParentClassViewPage() {
  const { studentId, classId } = useParams()
  const [record, setRecord] = useState(null)
  const [classInfo, setClassInfo] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const children = await fetchParentChildren()
      const child = children.find((c) => c.studentId === Number(studentId))
      const cls = child?.classes?.find((c) => c.classId === Number(classId))
      setClassInfo(cls || null)
      setRecord(await fetchChildStudyRecord(Number(studentId), Number(classId)))
    } catch (err) {
      setError(err.message)
      setRecord(null)
    } finally {
      setLoading(false)
    }
  }, [studentId, classId])

  useEffect(() => {
    load()
  }, [load])

  const gaugeTone = record ? GAUGE_CLASS[record.gaugeLevel] : null

  return (
    <div className="ams-parent">
      <Link to={`/parent/children/${studentId}`} className="ams-parent__back">
        ← {record?.studentName || '자녀'} 홈
      </Link>

      {loading ? (
        <p className="ams-class-list-page__empty">불러오는 중…</p>
      ) : error ? (
        <p className="ams-class-list-page__error">{error}</p>
      ) : record ? (
        <section className="ams-parent-record">
          <div className="ams-parent-record__head">
            <div>
              <h2 className="ams-parent-record__title">{classInfo?.name || '반'}</h2>
              {classInfo && (
                <span className="ams-parent-record__badge">{subjectLabel(classInfo.subject)}</span>
              )}
            </div>
          </div>

          <p className="ams-parent-record__readonly">읽기 전용 · {record.studentName}</p>

          <div className="ams-parent-record__score-block">
            <p className="ams-parent-record__score">
              {record.overallPercent}
              <span> %</span>
            </p>
            {gaugeTone && (
              <span className={`ams-gauge ams-gauge--${gaugeTone}`}>종합 달성률</span>
            )}
            {record.encouragementMessage && (
              <p className="ams-parent-record__message">{record.encouragementMessage}</p>
            )}
          </div>

          <ul className="ams-study-metric-list">
            <MetricRow label="숙제" metric={record.homework} />
            <MetricRow label="클리닉" metric={record.clinic} />
            <li className="ams-study-metric">
              <span>테스트</span>
              <span>
                {record.test?.latestSummary ||
                  (record.test?.scoredCount > 0 ? `평균 ${record.test.ratePercent}%` : '—')}
              </span>
            </li>
            <MetricRow label="영상 인증" metric={record.video} />
          </ul>
        </section>
      ) : null}
    </div>
  )
}
