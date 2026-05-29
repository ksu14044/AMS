import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchClasses } from '../../api/classesApi'
import { fetchMyStudyRecord } from '../../api/studyRecordsApi'
import { subjectLabel } from '../../auth/subjectLabels'
import { rememberLastClass } from '../../utils/lastClass'
import '../../styles/class-list.css'

const GAUGE_CLASS = { GREEN: 'green', ORANGE: 'orange', RED: 'red' }

export default function StudentRecordsPage() {
  const [classes, setClasses] = useState([])
  const [gauges, setGauges] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const list = await fetchClasses()
      setClasses(list)
      const gaugeMap = {}
      await Promise.all(
        list.map(async (c) => {
          try {
            gaugeMap[c.classId] = await fetchMyStudyRecord(c.classId)
          } catch {
            /* 개별 반 조회 실패는 게이지 생략 */
          }
        }),
      )
      setGauges(gaugeMap)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div className="ams-class-list-page">
      <p className="ams-class-list-page__desc">
        반별 숙제·클리닉·테스트·영상 달성률을 확인할 수 있습니다.
      </p>

      {loading ? (
        <p className="ams-class-list-page__empty">불러오는 중…</p>
      ) : error ? (
        <p className="ams-class-list-page__error">{error}</p>
      ) : classes.length === 0 ? (
        <p className="ams-class-list-page__empty">배정된 반이 없습니다.</p>
      ) : (
        <div className="ams-class-list">
          {classes.map((c) => {
            const record = gauges[c.classId]
            const gaugeTone = record ? GAUGE_CLASS[record.gaugeLevel] : null
            return (
              <Link
                key={c.classId}
                to={`/classes/${c.classId}?tab=study`}
                onClick={() => rememberLastClass(c.classId)}
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
                <span className="ams-class-list__meta">상세 보기 →</span>
              </Link>
            )
          })}
        </div>
      )}
    </div>
  )
}
