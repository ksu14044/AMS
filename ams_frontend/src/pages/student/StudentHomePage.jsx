import { Fragment, useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchClasses } from '../../api/classesApi'
import { fetchPendingTasks } from '../../api/notificationsApi'
import { fetchMyStudyRecord } from '../../api/studyRecordsApi'
import AcademyNoticesSection from '../../components/AcademyNoticesSection'
import DashboardCard from '../../components/DashboardCard'
import { SUBJECT_OPTIONS, subjectLabel } from '../../auth/subjectLabels'
import { rememberLastClass, useLastClassId } from '../../utils/lastClass'
import '../../styles/class-list.css'
import '../../styles/class-detail.css'
import '../../styles/notifications.css'

const GAUGE_CLASS = { GREEN: 'green', ORANGE: 'orange', RED: 'red' }

const TYPE_LABEL = {
  HOMEWORK: '숙제',
  TEST: '테스트',
  VIDEO: '영상',
  CLINIC: '클리닉',
}

const TAB_BY_TYPE = {
  HOMEWORK: 'homework',
  TEST: 'test',
  VIDEO: 'video',
  CLINIC: 'clinic',
}

const SUBJECT_ICONS = {
  KO: (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      <path d="M4 5.5A1.5 1.5 0 0 1 5.5 4H11v16H5.5A1.5 1.5 0 0 1 4 18.5v-13Z" />
      <path d="M20 5.5A1.5 1.5 0 0 0 18.5 4H13v16h5.5A1.5 1.5 0 0 0 20 18.5v-13Z" />
    </svg>
  ),
  EN: <span className="ams-subject-cards__glyph">Ax</span>,
  MATH: <span className="ams-subject-cards__glyph">(x)</span>,
}

export default function StudentHomePage() {
  const [classes, setClasses] = useState([])
  const [gauges, setGauges] = useState({})
  const [pendingTasks, setPendingTasks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [lastClassId, setLastClassId] = useLastClassId()

  const enrolledSubjects = useMemo(
    () => new Set(classes.map((c) => c.subject)),
    [classes],
  )

  const classesBySubject = useMemo(() => {
    const map = {}
    for (const c of classes) {
      ;(map[c.subject] ||= []).push(c)
    }
    return map
  }, [classes])

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [list, pending] = await Promise.all([
        fetchClasses(),
        fetchPendingTasks().catch(() => []),
      ])
      setClasses(list)
      setPendingTasks(pending)
      const gaugeMap = {}
      await Promise.all(
        list.map(async (c) => {
          try {
            gaugeMap[c.classId] = await fetchMyStudyRecord(c.classId)
          } catch {
            /* 개별 반 조회 실패는 카드만 게이지 생략 */
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

  function handleClassClick(classId) {
    rememberLastClass(classId)
    setLastClassId(String(classId))
  }

  return (
    <div className="ams-class-list-page">
      <AcademyNoticesSection canManage={false} compact />

      {!loading && pendingTasks.length > 0 && (
        <DashboardCard
          title="미완료 과제"
          actionLabel={`전체 ${pendingTasks.length}건 →`}
          actionTo="/notifications/pending"
        >
          <p className="ams-student-pending__summary">
            아직 완료하지 않은 과제가 {pendingTasks.length}건 있습니다.
          </p>
          <ul className="ams-student-pending__list">
            {pendingTasks.slice(0, 3).map((item) => (
              <li key={`${item.type}-${item.classId}-${item.entityId}`}>
                <Link
                  to={`/classes/${item.classId}?tab=${TAB_BY_TYPE[item.type] ?? 'home'}`}
                  className="ams-student-pending__link"
                >
                  <span className="ams-student-pending__label">
                    [{TYPE_LABEL[item.type] ?? item.type}] {item.title}
                  </span>
                  <span className="ams-student-pending__meta">
                    [{subjectLabel(item.subject)}] {item.className}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </DashboardCard>
      )}

      {loading ? (
        <p className="ams-class-list-page__empty">불러오는 중…</p>
      ) : error ? (
        <p className="ams-class-list-page__error">{error}</p>
      ) : classes.length === 0 ? (
        <div className="ams-class-list-page__card ams-class-list-page__card--accent">
          <h2 className="ams-class-list-page__title">수강반 배정 대기</h2>
          <p className="ams-class-list-page__desc">
            아직 배정된 반이 없습니다. 학원 관리자가 수강반을 배정하면 이 화면에 반 목록이
            표시됩니다.
          </p>
        </div>
      ) : (
        <>
          <p className="ams-class-list-page__lead">수강 과목</p>
          <div className="ams-subject-cards" aria-label="수강 과목">
            {SUBJECT_OPTIONS.map((option) => {
              const enrolled = enrolledSubjects.has(option.value)
              return (
                <div
                  key={option.value}
                  className={
                    enrolled
                      ? 'ams-subject-cards__item ams-subject-cards__item--enrolled'
                      : 'ams-subject-cards__item ams-subject-cards__item--muted'
                  }
                  aria-disabled={!enrolled}
                >
                  <span className="ams-subject-cards__icon" aria-hidden>
                    {SUBJECT_ICONS[option.value]}
                  </span>
                  <span>{option.label}</span>
                  {!enrolled && <span className="ams-subject-cards__hint">미수강</span>}
                </div>
              )
            })}
          </div>

          {SUBJECT_OPTIONS.map((option) => {
            const list = classesBySubject[option.value]
            if (!list || list.length === 0) return null
            return (
              <Fragment key={option.value}>
                <p className="ams-class-list-page__lead">
                  {subjectLabel(option.value)} 수강반
                </p>
                <div className="ams-class-list">
                  {list.map((c) => {
                    const record = gauges[c.classId]
                    const gaugeTone = record ? GAUGE_CLASS[record.gaugeLevel] : null
                    const isLast = lastClassId && String(c.classId) === lastClassId
                    return (
                      <Link
                        key={c.classId}
                        to={`/classes/${c.classId}`}
                        onClick={() => handleClassClick(c.classId)}
                        className={
                          isLast
                            ? 'ams-class-list__item ams-class-list__item--accent'
                            : 'ams-class-list__item'
                        }
                      >
                        <div className="ams-class-list__row">
                          <strong className="ams-class-list__name">{c.name}</strong>
                          {isLast && (
                            <span
                              className="ams-class-list__dot"
                              aria-label="최근 본 반"
                            />
                          )}
                          {!isLast && record && (
                            <span
                              className={`ams-gauge ams-gauge--${gaugeTone}`}
                              title={record.encouragementMessage}
                            >
                              종합 {record.overallPercent}%
                            </span>
                          )}
                        </div>
                        {c.classroom && (
                          <span className="ams-class-list__meta">{c.classroom}</span>
                        )}
                      </Link>
                    )
                  })}
                </div>
              </Fragment>
            )
          })}
        </>
      )}
    </div>
  )
}
