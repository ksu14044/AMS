import { useCallback, useEffect, useState } from 'react'
import {
  createTest,
  fetchTestScores,
  fetchTests,
  saveTestScores,
  toInstant,
} from '../../api/classesApi'

const STATUS_LABEL = { SCHEDULED: '예정', COMPLETED: '완료' }

export default function ClassTestSection({ classId, canManage, verifyOnly = false, onError }) {
  const [tests, setTests] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [scores, setScores] = useState([])
  const [scoreDraft, setScoreDraft] = useState({})
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [form, setForm] = useState({ title: '', testDate: '', testTime: '14:00' })

  const loadTests = useCallback(async () => {
    const list = await fetchTests(classId)
    setTests(list)
    if (list.length > 0 && !selectedId) {
      setSelectedId(String(list[0].testId))
    }
  }, [classId, selectedId])

  const loadScores = useCallback(async () => {
    if (!selectedId) {
      setScores([])
      setScoreDraft({})
      return
    }
    const rows = await fetchTestScores(classId, selectedId)
    setScores(rows)
    const draft = {}
    for (const r of rows) {
      draft[r.studentId] = { rawScore: r.rawScore ?? '', grade: r.grade ?? '' }
    }
    setScoreDraft(draft)
  }, [classId, selectedId])

  useEffect(() => {
    ;(async () => {
      setLoading(true)
      onError('')
      try {
        await loadTests()
      } catch (err) {
        onError(err.message)
      } finally {
        setLoading(false)
      }
    })()
  }, [loadTests, onError])

  useEffect(() => {
    loadScores().catch((err) => onError(err.message))
  }, [loadScores, onError])

  async function handleCreate(e) {
    e.preventDefault()
    if (!form.title.trim() || !form.testDate) return
    setSubmitting(true)
    onError('')
    try {
      const created = await createTest(classId, {
        title: form.title.trim(),
        testAt: toInstant(form.testDate, form.testTime),
      })
      setForm({ title: '', testDate: '', testTime: '14:00' })
      await loadTests()
      setSelectedId(String(created.testId))
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleSaveScores() {
    setSubmitting(true)
    onError('')
    try {
      const payload = scores.map((s) => {
        const d = scoreDraft[s.studentId] || {}
        return {
          studentId: s.studentId,
          rawScore: d.rawScore !== '' && d.rawScore != null ? Number(d.rawScore) : null,
          grade: d.grade || null,
        }
      })
      await saveTestScores(classId, selectedId, payload)
      await loadTests()
      await loadScores()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return <p className="ams-class-detail__empty">불러오는 중…</p>
  }

  const selectedTest = tests.find((t) => String(t.testId) === selectedId)

  return (
    <section className="ams-class-detail__section">
      <h3 className="ams-class-detail__heading">{verifyOnly ? '테스트 확인' : '테스트'}</h3>
      <p className="ams-class-detail__hint-inline">
        {verifyOnly
          ? '수업기록에서 등록한 테스트의 점수·석차를 확인합니다. 새 테스트는 수업기록 탭에서 등록하세요.'
          : '점수 저장 시 반 평균·상위 %가 계산되고 테스트가 완료 처리됩니다. 성실도 보고서는 「보고서」 탭에서 별도로 생성합니다.'}
      </p>

      {canManage && !verifyOnly && (
        <form className="ams-assignment-form" onSubmit={handleCreate}>
          <label>
            제목
            <input
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              maxLength={200}
              required
            />
          </label>
          <label>
            시험일
            <input
              type="date"
              value={form.testDate}
              onChange={(e) => setForm({ ...form, testDate: e.target.value })}
              required
            />
          </label>
          <label>
            시각
            <input
              type="time"
              value={form.testTime}
              onChange={(e) => setForm({ ...form, testTime: e.target.value })}
            />
          </label>
          <button type="submit" className="ams-btn ams-btn--primary" disabled={submitting}>
            테스트 등록
          </button>
        </form>
      )}

      {tests.length === 0 ? (
        <p className="ams-class-detail__empty">등록된 테스트가 없습니다.</p>
      ) : (
        <>
          <label className="ams-assignment-form__full">
            테스트 선택
            <select value={selectedId} onChange={(e) => setSelectedId(e.target.value)}>
              {tests.map((t) => (
                <option key={t.testId} value={t.testId}>
                  [{STATUS_LABEL[t.status] || t.status}] {t.title} —{' '}
                  {new Date(t.testAt).toLocaleString('ko-KR')}
                  {t.classAverage != null ? ` · 반평균 ${t.classAverage}` : ''}
                </option>
              ))}
            </select>
          </label>

          {selectedTest?.status === 'COMPLETED' && selectedTest.classAverage != null && (
            <p className="ams-class-detail__meta">
              반 평균 {selectedTest.classAverage}점
            </p>
          )}

          <div className="ams-submission-table-wrap">
            <table className="ams-submission-table">
              <thead>
                <tr>
                  <th>학생</th>
                  <th>점수</th>
                  <th>등급</th>
                  {!canManage && <th>상위%</th>}
                  {canManage && <th>저장 후 상위%</th>}
                </tr>
              </thead>
              <tbody>
                {scores.map((s) => (
                  <tr key={s.studentId}>
                    <td>{s.studentName}</td>
                    <td>
                      {canManage ? (
                        <input
                          type="number"
                          className="ams-submission-table__num"
                          value={scoreDraft[s.studentId]?.rawScore ?? ''}
                          disabled={submitting}
                          onChange={(e) =>
                            setScoreDraft((prev) => ({
                              ...prev,
                              [s.studentId]: { ...prev[s.studentId], rawScore: e.target.value },
                            }))
                          }
                        />
                      ) : (
                        (s.rawScore ?? '—')
                      )}
                    </td>
                    <td>
                      {canManage ? (
                        <input
                          type="text"
                          className="ams-submission-table__grade"
                          value={scoreDraft[s.studentId]?.grade ?? ''}
                          maxLength={16}
                          disabled={submitting}
                          onChange={(e) =>
                            setScoreDraft((prev) => ({
                              ...prev,
                              [s.studentId]: { ...prev[s.studentId], grade: e.target.value },
                            }))
                          }
                        />
                      ) : (
                        (s.grade ?? '—')
                      )}
                    </td>
                    <td>{s.upperRankPct != null ? `상위 ${s.upperRankPct}%` : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {canManage && selectedId && (
            <button
              type="button"
              className="ams-btn ams-btn--primary"
              disabled={submitting}
              onClick={handleSaveScores}
            >
              {submitting ? '저장 중…' : '점수 저장 (시험 완료)'}
            </button>
          )}
        </>
      )}
    </section>
  )
}
