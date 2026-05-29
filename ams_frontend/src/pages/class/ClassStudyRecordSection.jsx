import { useCallback, useEffect, useState } from 'react'
import {
  fetchMyStudyRecord,
  fetchStudentStudyRecord,
  fetchStudyRecordStudents,
} from '../../api/studyRecordsApi'

const GAUGE_LABEL = { GREEN: '우수', ORANGE: '보통', RED: '미달' }

function MetricRow({ label, metric, weightNote }) {
  const { completedCount, totalCount, ratePercent } = metric
  const detail =
    totalCount > 0 ? `${completedCount} / ${totalCount}` : '집계할 데이터 없음'
  return (
    <div className="ams-study-record__metric">
      <div className="ams-study-record__metric-head">
        <span className="ams-study-record__metric-label">
          {label}
          {weightNote && (
            <span className="ams-study-record__metric-weight">{weightNote}</span>
          )}
        </span>
        <strong className="ams-study-record__metric-pct">{ratePercent}%</strong>
      </div>
      <div
        className="ams-study-record__bar"
        role="progressbar"
        aria-valuenow={ratePercent}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={`${label} ${ratePercent}%`}
      >
        <span
          className="ams-study-record__bar-fill"
          style={{ width: `${Math.min(100, ratePercent)}%` }}
        />
      </div>
      <p className="ams-study-record__metric-detail">{detail}</p>
    </div>
  )
}

function TestMetricRow({ test, weightNote }) {
  const { ratePercent, attemptedCount, closedCount, latestSummary } = test
  const hasClosed = closedCount > 0
  const detail = hasClosed
    ? `응시 ${attemptedCount} / 마감 ${closedCount}`
    : '집계할 데이터 없음'

  return (
    <div className="ams-study-record__metric">
      <div className="ams-study-record__metric-head">
        <span className="ams-study-record__metric-label">
          테스트
          {weightNote && (
            <span className="ams-study-record__metric-weight">{weightNote}</span>
          )}
        </span>
        <strong className="ams-study-record__metric-pct">
          상대 성적 {hasClosed ? `${ratePercent}%` : '—'}
        </strong>
      </div>
      <div
        className="ams-study-record__bar"
        role="progressbar"
        aria-valuenow={hasClosed ? ratePercent : 0}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={`테스트 상대 성적 ${ratePercent}%`}
      >
        <span
          className="ams-study-record__bar-fill"
          style={{ width: `${hasClosed ? Math.min(100, ratePercent) : 0}%` }}
        />
      </div>
      <p className="ams-study-record__metric-detail">{detail}</p>
      {latestSummary && (
        <p className="ams-study-record__metric-detail ams-study-record__metric-detail--sub">
          최근 {latestSummary}
        </p>
      )}
      {hasClosed && attemptedCount < closedCount && (
        <p className="ams-study-record__metric-hint">
          미응시 시험은 상대 성적 0%로 반영됩니다.
        </p>
      )}
    </div>
  )
}

function StudyRecordPanel({ record }) {
  const gaugeClass = `ams-study-record__gauge ams-study-record__gauge--${record.gaugeLevel.toLowerCase()}`
  return (
    <div className="ams-study-record__panel">
      <div className={gaugeClass}>
        <p className="ams-study-record__gauge-pct">{record.overallPercent}%</p>
        <p className="ams-study-record__gauge-grade">
          종합 {record.overallGrade} · {GAUGE_LABEL[record.gaugeLevel]}
        </p>
        <p className="ams-study-record__gauge-msg">{record.encouragementMessage}</p>
      </div>

      <MetricRow label="숙제" metric={record.homework} weightNote="(40%)" />
      <MetricRow label="클리닉" metric={record.clinic} weightNote="(30%)" />
      <TestMetricRow test={record.test} weightNote="(30%)" />
      <MetricRow label="영상 인증" metric={record.video} />
      <p className="ams-study-record__note">
        종합 달성률은 숙제·클리닉·테스트만 반영합니다. 테스트 진행률은 반 내 상대 성적(높을수록
        우수) 평균이며, 미응시 시험은 0%로 포함됩니다. 영상 인증은 별도 지표입니다.
      </p>
    </div>
  )
}

export default function ClassStudyRecordSection({ classId, isStudent, onError }) {
  const [record, setRecord] = useState(null)
  const [students, setStudents] = useState([])
  const [selectedStudentId, setSelectedStudentId] = useState('')
  const [loading, setLoading] = useState(true)

  const loadStudentRecord = useCallback(
    async (studentId) => {
      const data = await fetchStudentStudyRecord(classId, studentId)
      setRecord(data)
    },
    [classId],
  )

  const load = useCallback(async () => {
    setLoading(true)
    onError('')
    try {
      if (isStudent) {
        const data = await fetchMyStudyRecord(classId)
        setRecord(data)
        return
      }
      const list = await fetchStudyRecordStudents(classId)
      setStudents(list)
      if (list.length === 0) {
        setRecord(null)
        return
      }
      const sid = String(list[0].studentId)
      setSelectedStudentId(sid)
      await loadStudentRecord(sid)
    } catch (err) {
      onError(err.message)
      setRecord(null)
    } finally {
      setLoading(false)
    }
  }, [classId, isStudent, loadStudentRecord, onError])

  useEffect(() => {
    load()
  }, [load])

  async function handleStudentChange(e) {
    const sid = e.target.value
    setSelectedStudentId(sid)
    setLoading(true)
    onError('')
    try {
      await loadStudentRecord(sid)
    } catch (err) {
      onError(err.message)
      setRecord(null)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return <p className="ams-class-detail__empty">불러오는 중…</p>
  }

  if (!isStudent && students.length === 0) {
    return <p className="ams-class-detail__empty">수강생이 없어 공부기록을 표시할 수 없습니다.</p>
  }

  return (
    <section className="ams-study-record">
      {!isStudent && students.length > 0 && (
        <label className="ams-study-record__select">
          <span>학생</span>
          <select value={selectedStudentId} onChange={handleStudentChange}>
            {students.map((s) => (
              <option key={s.studentId} value={String(s.studentId)}>
                {s.studentName}
              </option>
            ))}
          </select>
        </label>
      )}

      {record && <StudyRecordPanel record={record} />}
    </section>
  )
}
