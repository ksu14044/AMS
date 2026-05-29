import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchClasses } from '../../api/classesApi'
import { subjectLabel } from '../../auth/subjectLabels'
import { rememberLastClass } from '../../utils/lastClass'
import '../../styles/class-list.css'

export default function StudentClinicPage() {
  const [classes, setClasses] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setClasses(await fetchClasses())
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
        반을 선택하면 이번 주 클리닉 슬롯을 예약할 수 있습니다.
      </p>

      {loading ? (
        <p className="ams-class-list-page__empty">불러오는 중…</p>
      ) : error ? (
        <p className="ams-class-list-page__error">{error}</p>
      ) : classes.length === 0 ? (
        <p className="ams-class-list-page__empty">배정된 반이 없습니다.</p>
      ) : (
        <div className="ams-class-list">
          {classes.map((c) => (
            <Link
              key={c.classId}
              to={`/classes/${c.classId}?tab=clinic`}
              onClick={() => rememberLastClass(c.classId)}
              className="ams-class-list__item"
            >
              <div className="ams-class-list__row">
                <strong className="ams-class-list__name">{c.name}</strong>
                <span className="ams-class-list__badge">{subjectLabel(c.subject)}</span>
              </div>
              <span className="ams-class-list__meta">클리닉 예약 →</span>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
