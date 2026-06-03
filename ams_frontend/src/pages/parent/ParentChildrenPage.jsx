import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { fetchParentChildren } from '../../api/parentApi'
import { subjectLabel } from '../../auth/subjectLabels'
import { nameInitial } from '../../components/parent/parentUtils'
import '../../styles/class-list.css'
import '../../styles/parent.css'

export default function ParentChildrenPage() {
  const { user } = useAuth()
  const [children, setChildren] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setChildren(await fetchParentChildren())
    } catch (err) {
      setError(err.message)
      setChildren([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div className="ams-parent">
      <section className="ams-parent__hero">
        <h2 className="ams-parent__hero-title">
          {user?.name ? `${user.name}님, 안녕하세요` : '학부모 홈'}
        </h2>
        <p className="ams-parent__hero-desc">
          연결된 자녀의 미완료 과제·공부기록·성실도 보고서를 확인할 수 있습니다. 수정은 할 수
          없으며 조회만 가능합니다.
        </p>
      </section>

      <p className="ams-parent__section-title">연결된 자녀</p>

      {loading ? (
        <p className="ams-class-list-page__empty">불러오는 중…</p>
      ) : error ? (
        <p className="ams-class-list-page__error">{error}</p>
      ) : children.length === 0 ? (
        <div className="ams-parent-empty-card">
          <p className="ams-parent-empty-card__title">연결된 자녀가 없습니다</p>
          <p className="ams-parent-empty-card__desc">
            학원에 자녀 연결을 요청해 주세요. 교직원이 학생부에서 연결하면 이곳에 표시됩니다.
          </p>
        </div>
      ) : (
        <ul className="ams-parent-children">
          {children.map((child) => {
            const classCount = child.classes?.length ?? 0
            return (
              <li key={child.studentId}>
                <Link
                  to={`/parent/children/${child.studentId}`}
                  className="ams-parent-child-card"
                >
                  <span className="ams-parent-child-card__avatar" aria-hidden>
                    {nameInitial(child.studentName)}
                  </span>
                  <span className="ams-parent-child-card__body">
                    <span className="ams-parent-child-card__name">{child.studentName}</span>
                    <span className="ams-parent-child-card__meta">
                      {classCount > 0 ? `${classCount}개 반 수강` : '배정된 반 없음'}
                    </span>
                    {classCount > 0 && (
                      <span className="ams-parent-child-card__chips">
                        {child.classes.slice(0, 4).map((c) => (
                          <span key={c.classId} className="ams-parent-child-card__chip">
                            {c.name} · {subjectLabel(c.subject)}
                          </span>
                        ))}
                        {classCount > 4 && (
                          <span className="ams-parent-child-card__chip">
                            +{classCount - 4}
                          </span>
                        )}
                      </span>
                    )}
                  </span>
                  <span className="ams-parent-child-card__chevron" aria-hidden>
                    ›
                  </span>
                </Link>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
