import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  createLessonRecord,
  fetchClinicAssistants,
  fetchLessonRecord,
  fetchLessonRecords,
  updateLessonRecord,
} from '../../api/classesApi'
import { dayLabel } from '../../auth/dayLabels'

const JS_DAY_TO_CLINIC = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']

function dayOfWeekFromDate(dateStr) {
  if (!dateStr) return 'MON'
  const d = new Date(`${dateStr}T12:00:00`)
  return JS_DAY_TO_CLINIC[d.getDay()]
}

const EMPTY_CREATE = {
  lessonDate: '',
  summary: '',
  includeHomework: false,
  homeworkTitle: '',
  homeworkQuestionCount: '',
  includeTest: false,
  testTitle: '',
  testQuestionCount: '',
  testRetakeThresholdCount: '',
  includeVideo: false,
  videoTitle: '',
  youtubeUrl: '',
  includeClinic: false,
  clinicDate: '',
  clinicStartTime: '18:00',
  clinicAssistantId: '',
  clinicMaxCapacity: '10',
}

const LINKED_META = {
  homework: { label: '숙제', tone: 'homework' },
  test: { label: '테스트', tone: 'test' },
  video: { label: '영상', tone: 'video' },
  clinic: { label: '클리닉', tone: 'clinic' },
}

function formatLessonDate(dateStr) {
  if (!dateStr) return '—'
  const [y, m, d] = dateStr.split('-').map(Number)
  return `${y}.${String(m).padStart(2, '0')}.${String(d).padStart(2, '0')}`
}

function formatDateTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  const y = d.getFullYear()
  const mo = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}.${mo}.${day} ${h}:${min}`
}

function linkedBadges(record) {
  const badges = []
  if (record.homeworkCount > 0) {
    badges.push({ key: 'homework', label: '숙제', count: record.homeworkCount })
  }
  if (record.testCount > 0) {
    badges.push({ key: 'test', label: '테스트', count: record.testCount })
  }
  if (record.videoCount > 0) {
    badges.push({ key: 'video', label: '영상', count: record.videoCount })
  }
  if (record.clinicCount > 0) {
    badges.push({ key: 'clinic', label: '클리닉', count: record.clinicCount })
  }
  return badges
}

function linkedTypeLabel(type) {
  return LINKED_META[type]?.label ?? type
}

function LinkedBadgeGroup({ record }) {
  const badges = linkedBadges(record)
  if (badges.length === 0) {
    return <span className="ams-lesson-board__empty-tag">없음</span>
  }
  return (
    <div className="ams-lesson-board__badges">
      {badges.map((b) => (
        <span key={b.key} className={`ams-lesson-board__badge ams-lesson-board__badge--${b.key}`}>
          {b.label}
          {b.count > 1 ? ` ${b.count}` : ''}
        </span>
      ))}
    </div>
  )
}

function OptionCard({ active, label, description, onToggle, children }) {
  return (
    <div className={`ams-lesson-board__option${active ? ' ams-lesson-board__option--active' : ''}`}>
      <button type="button" className="ams-lesson-board__option-head" onClick={onToggle}>
        <span className="ams-lesson-board__option-check" aria-hidden />
        <span className="ams-lesson-board__option-text">
          <strong>{label}</strong>
          <span>{description}</span>
        </span>
      </button>
      {active && children && <div className="ams-lesson-board__option-body">{children}</div>}
    </div>
  )
}

export default function ClassLessonRecordSection({ classId, canEdit, onError }) {
  const [records, setRecords] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [selectedId, setSelectedId] = useState('')
  const [detail, setDetail] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [showWrite, setShowWrite] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_CREATE)
  const [editSummary, setEditSummary] = useState('')
  const [assistants, setAssistants] = useState([])

  const load = useCallback(async () => {
    setLoading(true)
    onError('')
    try {
      const list = await fetchLessonRecords(classId)
      setRecords(list)
      setSelectedId((prev) => {
        if (prev && list.some((r) => String(r.lessonRecordId) === prev)) {
          return prev
        }
        return list.length > 0 ? String(list[0].lessonRecordId) : ''
      })
    } catch (err) {
      onError(err.message)
      setRecords([])
    } finally {
      setLoading(false)
    }
  }, [classId, onError])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (!canEdit) return
    fetchClinicAssistants(classId)
      .then(setAssistants)
      .catch((err) => onError(err.message))
  }, [classId, canEdit, onError])

  useEffect(() => {
    if (!selectedId) {
      setDetail(null)
      setEditSummary('')
      return
    }
    let cancelled = false
    ;(async () => {
      setDetailLoading(true)
      onError('')
      try {
        const row = await fetchLessonRecord(classId, selectedId)
        if (!cancelled) {
          setDetail(row)
          setEditSummary(row.summary ?? '')
        }
      } catch (err) {
        if (!cancelled) {
          onError(err.message)
          setDetail(null)
        }
      } finally {
        if (!cancelled) setDetailLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [classId, selectedId, onError])

  const boardRows = useMemo(
    () =>
      records.map((r, index) => ({
        ...r,
        rowNo: records.length - index,
      })),
    [records],
  )

  function closeWrite() {
    setShowWrite(false)
    setCreateForm(EMPTY_CREATE)
  }

  async function handleCreate(e) {
    e.preventDefault()
    if (!createForm.lessonDate || !createForm.summary.trim()) return
    if (createForm.includeHomework && !createForm.homeworkTitle.trim()) return
    if (createForm.includeHomework && !createForm.homeworkQuestionCount) return
    if (createForm.includeTest && !createForm.testTitle.trim()) return
    if (createForm.includeTest && (!createForm.testQuestionCount || !createForm.testRetakeThresholdCount)) return
    if (createForm.includeVideo && (!createForm.videoTitle.trim() || !createForm.youtubeUrl.trim())) {
      return
    }
    if (createForm.includeClinic) {
      if (!createForm.clinicDate || !createForm.clinicAssistantId) return
    }

    const payload = {
      lessonDate: createForm.lessonDate,
      summary: createForm.summary.trim(),
    }
    if (createForm.includeHomework) {
      payload.homework = {
        title: createForm.homeworkTitle.trim(),
        questionCount: Number(createForm.homeworkQuestionCount),
      }
    }
    if (createForm.includeTest) {
      payload.test = {
        title: createForm.testTitle.trim(),
        questionCount: Number(createForm.testQuestionCount),
        retakeThresholdCount: Number(createForm.testRetakeThresholdCount),
      }
    }
    if (createForm.includeVideo) {
      payload.video = {
        title: createForm.videoTitle.trim(),
        youtubeUrl: createForm.youtubeUrl.trim(),
      }
    }
    if (createForm.includeClinic) {
      payload.clinic = {
        clinicDate: createForm.clinicDate,
        startTime: createForm.clinicStartTime,
        assistantId: Number(createForm.clinicAssistantId),
        maxCapacity: Number(createForm.clinicMaxCapacity) || 10,
      }
    }

    setSubmitting(true)
    onError('')
    try {
      const created = await createLessonRecord(classId, payload)
      closeWrite()
      await load()
      setSelectedId(String(created.lessonRecordId))
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleUpdateSummary(e) {
    e.preventDefault()
    if (!selectedId || !editSummary.trim()) return
    setSubmitting(true)
    onError('')
    try {
      const updated = await updateLessonRecord(classId, selectedId, { summary: editSummary.trim() })
      setDetail(updated)
      await load()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="ams-lesson-board ams-lesson-board--loading">
        <p className="ams-class-detail__empty">불러오는 중…</p>
      </div>
    )
  }

  return (
    <section className="ams-lesson-board">
      <header className="ams-lesson-board__header">
        <div className="ams-lesson-board__header-text">
          <div className="ams-lesson-board__title-row">
            <h3 className="ams-lesson-board__title">수업기록</h3>
            <span className="ams-pill ams-pill--muted">{records.length}건</span>
          </div>
          <p className="ams-lesson-board__lead">
            수업일·요약과 숙제·테스트·영상·클리닉을 한 번에 등록합니다. 점수 확인은 숙제·테스트
            탭을 이용하세요.
          </p>
        </div>
        {canEdit && (
          <button
            type="button"
            className={`ams-btn ams-btn--primary ams-btn--sm${showWrite ? ' ams-lesson-board__write-toggle--open' : ''}`}
            onClick={() => (showWrite ? closeWrite() : setShowWrite(true))}
          >
            {showWrite ? '작성 취소' : '+ 수업기록 작성'}
          </button>
        )}
      </header>

      {canEdit && showWrite && (
        <form className="ams-lesson-board__compose ams-card ams-card--elevated" onSubmit={handleCreate}>
          <div className="ams-lesson-board__compose-head">
            <h4 className="ams-lesson-board__compose-title">새 수업기록</h4>
            <p className="ams-lesson-board__compose-desc">수업 내용과 함께 등록할 항목을 선택하세요.</p>
          </div>

          <div className="ams-lesson-board__compose-grid">
            <div className="ams-lesson-board__compose-main">
              <label className="ams-field">
                <span className="ams-field__label">수업일</span>
                <input
                  className="ams-field__input"
                  type="date"
                  value={createForm.lessonDate}
                  onChange={(e) => setCreateForm({ ...createForm, lessonDate: e.target.value })}
                  required
                />
              </label>
              <label className="ams-field">
                <span className="ams-field__label">수업 내용 요약</span>
                <textarea
                  className="ams-field__textarea"
                  value={createForm.summary}
                  onChange={(e) => setCreateForm({ ...createForm, summary: e.target.value })}
                  rows={5}
                  placeholder="오늘 수업에서 다룬 단원, 핵심 개념, 다음 수업 안내 등을 적어 주세요."
                  required
                />
              </label>
            </div>

            <div className="ams-lesson-board__compose-side">
              <p className="ams-lesson-board__compose-side-title">함께 등록할 항목</p>
              <div className="ams-lesson-board__options">
                <OptionCard
                  active={createForm.includeHomework}
                  label="숙제"
                  description="반 전원 대상"
                  onToggle={() =>
                    setCreateForm({ ...createForm, includeHomework: !createForm.includeHomework })
                  }
                >
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">숙제 제목</span>
                    <input
                      className="ams-field__input"
                      value={createForm.homeworkTitle}
                      onChange={(e) =>
                        setCreateForm({ ...createForm, homeworkTitle: e.target.value })
                      }
                      maxLength={200}
                      placeholder="예: 3단원 워크북 p.42~45"
                      required
                    />
                  </label>
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">문항 수</span>
                    <input
                      className="ams-field__input"
                      type="number"
                      min={1}
                      value={createForm.homeworkQuestionCount}
                      onChange={(e) =>
                        setCreateForm({ ...createForm, homeworkQuestionCount: e.target.value })
                      }
                      placeholder="예: 20"
                      required
                    />
                  </label>
                </OptionCard>

                <OptionCard
                  active={createForm.includeTest}
                  label="테스트"
                  description="반 전원 대상"
                  onToggle={() => setCreateForm({ ...createForm, includeTest: !createForm.includeTest })}
                >
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">테스트 제목</span>
                    <input
                      className="ams-field__input"
                      value={createForm.testTitle}
                      onChange={(e) => setCreateForm({ ...createForm, testTitle: e.target.value })}
                      maxLength={200}
                      placeholder="예: 3단원 형성평가"
                      required
                    />
                  </label>
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">문항 수</span>
                    <input
                      className="ams-field__input"
                      type="number"
                      min={1}
                      value={createForm.testQuestionCount}
                      onChange={(e) =>
                        setCreateForm({ ...createForm, testQuestionCount: e.target.value })
                      }
                      required
                    />
                  </label>
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">합격 기준 (맞은 문항 수)</span>
                    <input
                      className="ams-field__input"
                      type="number"
                      min={1}
                      value={createForm.testRetakeThresholdCount}
                      onChange={(e) =>
                        setCreateForm({ ...createForm, testRetakeThresholdCount: e.target.value })
                      }
                      placeholder="이 미만이면 재시험"
                      required
                    />
                  </label>
                </OptionCard>

                <OptionCard
                  active={createForm.includeVideo}
                  label="영상"
                  description="YouTube 링크"
                  onToggle={() => setCreateForm({ ...createForm, includeVideo: !createForm.includeVideo })}
                >
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">영상 제목</span>
                    <input
                      className="ams-field__input"
                      value={createForm.videoTitle}
                      onChange={(e) => setCreateForm({ ...createForm, videoTitle: e.target.value })}
                      maxLength={200}
                      placeholder="예: 3단원 개념 정리"
                      required
                    />
                  </label>
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">YouTube URL</span>
                    <input
                      className="ams-field__input"
                      type="url"
                      value={createForm.youtubeUrl}
                      onChange={(e) => setCreateForm({ ...createForm, youtubeUrl: e.target.value })}
                      placeholder="https://www.youtube.com/watch?v=..."
                      required
                    />
                  </label>
                </OptionCard>

                <OptionCard
                  active={createForm.includeClinic}
                  label="클리닉"
                  description="날짜·시간·조교 슬롯"
                  onToggle={() => {
                    const next = !createForm.includeClinic
                    setCreateForm({
                      ...createForm,
                      includeClinic: next,
                      clinicDate:
                        next && !createForm.clinicDate && createForm.lessonDate
                          ? createForm.lessonDate
                          : createForm.clinicDate,
                    })
                  }}
                >
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">클리닉 날짜</span>
                    <input
                      className="ams-field__input"
                      type="date"
                      value={createForm.clinicDate}
                      onChange={(e) => setCreateForm({ ...createForm, clinicDate: e.target.value })}
                      required
                    />
                  </label>
                  {createForm.clinicDate && (
                    <p className="ams-lesson-board__field-hint">
                      {dayLabel(dayOfWeekFromDate(createForm.clinicDate))}요일 클리닉
                    </p>
                  )}
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">시작 시각</span>
                    <input
                      className="ams-field__input"
                      type="time"
                      value={createForm.clinicStartTime}
                      onChange={(e) =>
                        setCreateForm({ ...createForm, clinicStartTime: e.target.value })
                      }
                      required
                    />
                  </label>
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">담당 조교</span>
                    <select
                      className="ams-field__input"
                      value={createForm.clinicAssistantId}
                      onChange={(e) =>
                        setCreateForm({ ...createForm, clinicAssistantId: e.target.value })
                      }
                      required
                    >
                      <option value="">조교 선택</option>
                      {assistants.map((a) => (
                        <option key={a.userId} value={a.userId}>
                          {a.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">정원</span>
                    <input
                      className="ams-field__input"
                      type="number"
                      min={1}
                      max={20}
                      value={createForm.clinicMaxCapacity}
                      onChange={(e) =>
                        setCreateForm({ ...createForm, clinicMaxCapacity: e.target.value })
                      }
                    />
                  </label>
                </OptionCard>
              </div>
            </div>
          </div>

          <div className="ams-lesson-board__compose-foot">
            <button type="button" className="ams-btn ams-btn--ghost ams-btn--sm" onClick={closeWrite}>
              취소
            </button>
            <button type="submit" className="ams-btn ams-btn--primary ams-btn--sm" disabled={submitting}>
              {submitting ? '등록 중…' : '수업기록 등록'}
            </button>
          </div>
        </form>
      )}

      <div className="ams-lesson-board__layout">
        <div className="ams-lesson-board__list ams-card ams-card--elevated">
          <div className="ams-lesson-board__list-head">
            <h4 className="ams-lesson-board__panel-title">목록</h4>
            <span className="ams-lesson-board__list-meta">최신순</span>
          </div>

          {records.length === 0 ? (
            <div className="ams-lesson-board__empty-state">
              <p className="ams-lesson-board__empty-title">등록된 수업기록이 없습니다</p>
              <p className="ams-lesson-board__empty-desc">
                {canEdit
                  ? '「+ 수업기록 작성」으로 첫 기록을 남겨 보세요.'
                  : '담당 교직원이 수업기록을 등록하면 여기에 표시됩니다.'}
              </p>
            </div>
          ) : (
            <div className="ams-lesson-board__table-wrap">
              <table className="ams-lesson-board__table">
                <thead>
                  <tr>
                    <th scope="col" className="ams-lesson-board__col-no">
                      번호
                    </th>
                    <th scope="col">수업일</th>
                    <th scope="col">요약</th>
                    <th scope="col">귀속 항목</th>
                    <th scope="col">작성자</th>
                    <th scope="col">등록일</th>
                  </tr>
                </thead>
                <tbody>
                  {boardRows.map((r) => {
                    const active = selectedId === String(r.lessonRecordId)
                    return (
                      <tr
                        key={r.lessonRecordId}
                        className={`ams-lesson-board__row${active ? ' ams-lesson-board__row--active' : ''}`}
                        onClick={() => setSelectedId(String(r.lessonRecordId))}
                        aria-selected={active}
                      >
                        <td className="ams-lesson-board__col-no">
                          <span className="ams-lesson-board__row-no">{r.rowNo}</span>
                        </td>
                        <td className="ams-lesson-board__col-date">{formatLessonDate(r.lessonDate)}</td>
                        <td className="ams-lesson-board__summary-cell" title={r.summary}>
                          {r.summary}
                        </td>
                        <td>
                          <LinkedBadgeGroup record={r} />
                        </td>
                        <td className="ams-lesson-board__col-author">{r.authorName}</td>
                        <td className="ams-lesson-board__col-created">{formatDateTime(r.createdAt)}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <aside className="ams-lesson-board__detail ams-card ams-card--elevated">
          {!selectedId ? (
            <div className="ams-lesson-board__empty-state ams-lesson-board__empty-state--detail">
              <p className="ams-lesson-board__empty-title">수업기록을 선택하세요</p>
              <p className="ams-lesson-board__empty-desc">왼쪽 목록에서 항목을 클릭하면 상세 내용이 표시됩니다.</p>
            </div>
          ) : detailLoading ? (
            <p className="ams-class-detail__empty">불러오는 중…</p>
          ) : detail ? (
            <>
              <div className="ams-lesson-board__detail-head">
                <time className="ams-lesson-board__detail-date">{formatLessonDate(detail.lessonDate)}</time>
                <div className="ams-lesson-board__detail-meta">
                  <span>{detail.authorName}</span>
                  <span aria-hidden>·</span>
                  <span>{formatDateTime(detail.createdAt)}</span>
                </div>
              </div>

              <LinkedBadgeGroup record={detail} />

              <div className="ams-lesson-board__detail-section">
                <h5 className="ams-lesson-board__detail-label">수업 요약</h5>
                {canEdit ? (
                  <form className="ams-lesson-board__edit-form" onSubmit={handleUpdateSummary}>
                    <textarea
                      className="ams-field__textarea ams-lesson-board__detail-textarea"
                      value={editSummary}
                      onChange={(e) => setEditSummary(e.target.value)}
                      rows={8}
                      required
                    />
                    <button
                      type="submit"
                      className="ams-btn ams-btn--primary ams-btn--sm"
                      disabled={submitting}
                    >
                      {submitting ? '저장 중…' : '요약 저장'}
                    </button>
                  </form>
                ) : (
                  <p className="ams-lesson-board__detail-body">{detail.summary}</p>
                )}
              </div>

              <div className="ams-lesson-board__detail-section">
                <h5 className="ams-lesson-board__detail-label">귀속 항목</h5>
                {detail.linkedItems?.length > 0 ? (
                  <ul className="ams-lesson-board__linked-items">
                    {detail.linkedItems.map((item) => (
                      <li
                        key={`${item.type}-${item.id}`}
                        className={`ams-lesson-board__linked-item ams-lesson-board__linked-item--${item.type}`}
                      >
                        <span className="ams-lesson-board__linked-item-type">
                          {linkedTypeLabel(item.type)}
                        </span>
                        <span className="ams-lesson-board__linked-item-title">{item.title}</span>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="ams-lesson-board__empty-tag">연결된 항목 없음</p>
                )}
              </div>
            </>
          ) : (
            <p className="ams-class-detail__empty">수업기록을 불러올 수 없습니다.</p>
          )}
        </aside>
      </div>
    </section>
  )
}
