import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  fetchClassSchedule,
  fetchClinicWeek,
  fetchHomeworkSubmissions,
  fetchHomeworks,
  fetchTests,
} from '../../api/classesApi'
import { fetchMyStudyRecord } from '../../api/studyRecordsApi'
import { dayLabel } from '../../auth/dayLabels'
import { mondayOfWeek } from '../../utils/weekDate'

const STATUS_LABEL = { SCHEDULED: '예정', COMPLETED: '완료' }

function formatScheduleSummary(slots) {
  if (!slots?.length) {
    return '등록된 수업 일정이 없습니다.'
  }
  const days = slots.map((s) => dayLabel(s.dayOfWeek)).join('·')
  const first = slots[0]
  const start = first.startTime?.slice(0, 5) ?? ''
  const end = first.endTime?.slice(0, 5) ?? ''
  const room = first.room || slots.find((s) => s.room)?.room
  const time = start && end ? `${start}–${end}` : start
  return `매주 ${days} ${time}${room ? ` / ${room}` : ''}`
}

function formatDue(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('ko-KR', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function HomeCard({ title, actionLabel, onAction, children }) {
  return (
    <section className="ams-class-home__card">
      <header className="ams-class-home__card-head">
        <h3 className="ams-class-home__card-title">{title}</h3>
        {actionLabel && onAction && (
          <button type="button" className="ams-class-home__link" onClick={onAction}>
            {actionLabel}
          </button>
        )}
      </header>
      {children}
    </section>
  )
}

export default function ClassHomeSection({ classId, notices, onGoTab, onError }) {
  const [loading, setLoading] = useState(true)
  const [studyRecord, setStudyRecord] = useState(null)
  const [schedule, setSchedule] = useState([])
  const [homeworks, setHomeworks] = useState([])
  const [tests, setTests] = useState([])
  const [myClinics, setMyClinics] = useState([])
  const [homeworkSubmit, setHomeworkSubmit] = useState(null)

  const latestNotice = notices?.[0] ?? null

  const scheduledHomeworks = useMemo(
    () => homeworks.filter((h) => h.status === 'SCHEDULED').slice(0, 2),
    [homeworks],
  )
  const completedHomeworks = useMemo(
    () => homeworks.filter((h) => h.status === 'COMPLETED').slice(0, 1),
    [homeworks],
  )
  const scheduledTests = useMemo(
    () => tests.filter((t) => t.status === 'SCHEDULED').slice(0, 2),
    [tests],
  )

  const load = useCallback(async () => {
    setLoading(true)
    onError('')
    try {
      const weekStart = mondayOfWeek()
      const [record, scheduleRows, hwList, testList, weekView] = await Promise.all([
        fetchMyStudyRecord(classId),
        fetchClassSchedule(classId),
        fetchHomeworks(classId),
        fetchTests(classId),
        fetchClinicWeek(classId, weekStart),
      ])
      setStudyRecord(record)
      setSchedule(scheduleRows)
      setHomeworks(hwList)
      setTests(testList)

      const mine =
        weekView?.slots
          ?.filter((row) => row.myReservationId)
          .map((row) => {
            const s = row.slot
            const time = s.startTime?.slice(0, 5) ?? ''
            const assistant = s.assistantName || '조교'
            return {
              slotId: s.slotId,
              day: s.dayOfWeek,
              time,
              assistant,
              label: `${dayLabel(s.dayOfWeek)} ${time}`,
            }
          }) ?? []
      setMyClinics(mine)

      const nextHw = hwList.find((h) => h.status === 'SCHEDULED')
      if (nextHw) {
        try {
          const rows = await fetchHomeworkSubmissions(classId, nextHw.homeworkId)
          setHomeworkSubmit({
            homeworkId: nextHw.homeworkId,
            submitted: rows[0]?.submitted ?? false,
          })
        } catch {
          setHomeworkSubmit(null)
        }
      } else {
        setHomeworkSubmit(null)
      }
    } catch (err) {
      onError(err.message)
    } finally {
      setLoading(false)
    }
  }, [classId, onError])

  useEffect(() => {
    load()
  }, [load])

  if (loading) {
    return <p className="ams-class-detail__empty">불러오는 중…</p>
  }

  return (
    <div className="ams-class-home">
      <section className="ams-class-home__highlight">
        <header className="ams-class-home__highlight-head">
          <button
            type="button"
            className="ams-class-home__highlight-title"
            onClick={() => onGoTab('clinic')}
          >
            <span aria-hidden>📅</span> 내 클리닉
          </button>
          <button
            type="button"
            className="ams-btn ams-btn--primary ams-btn--sm"
            onClick={() => onGoTab('study')}
          >
            <span aria-hidden>📊</span> 공부기록
          </button>
        </header>

        {myClinics.length > 0 ? (
          <ul className="ams-class-home__highlight-slots" aria-label="이번 주 내 클리닉">
            {myClinics.map((c) => (
              <li key={c.slotId}>
                <span className="ams-pill ams-pill--primary">{c.label}</span>
              </li>
            ))}
          </ul>
        ) : (
          <p className="ams-class-home__highlight-empty">
            이번 주에 예약한 클리닉이 없습니다.{' '}
            <button
              type="button"
              className="ams-class-home__highlight-cta"
              onClick={() => onGoTab('clinic')}
            >
              지금 예약 →
            </button>
          </p>
        )}

        {studyRecord && (
          <div className="ams-class-home__sincerity">
            <div className="ams-class-home__sincerity-row">
              <span className="ams-class-home__sincerity-label">성실도</span>
              <strong className="ams-class-home__sincerity-pct">
                {studyRecord.overallPercent}%
              </strong>
            </div>
            <div
              className="ams-progress"
              role="progressbar"
              aria-valuenow={studyRecord.overallPercent}
              aria-valuemin={0}
              aria-valuemax={100}
              aria-label={`성실도 ${studyRecord.overallPercent}%`}
            >
              <span
                className="ams-progress__fill"
                style={{ width: `${Math.min(100, studyRecord.overallPercent)}%` }}
              />
            </div>
          </div>
        )}
      </section>

      <HomeCard title="수업 정보" actionLabel="더보기 →" onAction={() => onGoTab('notices')}>
        <p className="ams-class-home__text">{formatScheduleSummary(schedule)}</p>
      </HomeCard>

      <HomeCard title="최근 공지" actionLabel="전체 →" onAction={() => onGoTab('notices')}>
        {latestNotice ? (
          <>
            <p className="ams-class-home__strong">{latestNotice.title}</p>
            <p className="ams-class-home__text ams-class-home__text--clamp">{latestNotice.body}</p>
            <p className="ams-class-home__meta">
              {new Date(latestNotice.publishedAt).toLocaleDateString('ko-KR')}
            </p>
          </>
        ) : (
          <p className="ams-class-home__empty">등록된 공지가 없습니다.</p>
        )}
      </HomeCard>

      <HomeCard title="숙제" actionLabel="전체 →" onAction={() => onGoTab('homework')}>
        {scheduledHomeworks.length === 0 && completedHomeworks.length === 0 ? (
          <p className="ams-class-home__empty">등록된 숙제가 없습니다.</p>
        ) : (
          <ul className="ams-class-home__list">
            {scheduledHomeworks.map((h) => (
              <li key={h.homeworkId}>
                <span className="ams-class-home__tag">{STATUS_LABEL[h.status]}</span>
                {h.title}
                {h.questionCount ? (
                  <span className="ams-class-home__meta"> · {h.questionCount}문항</span>
                ) : null}
                {homeworkSubmit?.homeworkId === h.homeworkId && homeworkSubmit.submitted && (
                  <span className="ams-class-home__badge">제출 완료</span>
                )}
              </li>
            ))}
            {completedHomeworks.map((h) => (
              <li key={h.homeworkId}>
                <span className="ams-class-home__tag ams-class-home__tag--done">
                  {STATUS_LABEL[h.status]}
                </span>
                {h.title}
              </li>
            ))}
          </ul>
        )}
      </HomeCard>

      <HomeCard title="테스트" actionLabel="전체 →" onAction={() => onGoTab('test')}>
        {scheduledTests.length === 0 ? (
          <p className="ams-class-home__empty">예정된 테스트가 없습니다.</p>
        ) : (
          <ul className="ams-class-home__list">
            {scheduledTests.map((t) => (
              <li key={t.testId}>
                <span className="ams-class-home__tag">{STATUS_LABEL[t.status]}</span>
                {t.title}
                <span className="ams-class-home__meta"> {formatDue(t.testAt)}</span>
              </li>
            ))}
          </ul>
        )}
      </HomeCard>
    </div>
  )
}
