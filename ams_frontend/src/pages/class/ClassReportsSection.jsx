import { useCallback, useEffect, useMemo, useState } from 'react'
import { fetchStudyRecordStudents } from '../../api/studyRecordsApi'
import {
  createReportPeriodPreset,
  deleteReportPeriodPreset,
  downloadPeriodReportsArchive,
  fetchClassReports,
  fetchReportPeriodPresets,
  formatLocalDate,
  formatReportPeriod,
  generateReports,
  updateReportPeriodPreset,
} from '../../api/reportsApi'
import ReportDetailModal from '../../components/ReportDetailModal'
import StudentTargetPicker from '../../components/StudentTargetPicker'
import { createInitialTarget } from '../../utils/assignmentTargets'
import { captureAndUploadReportImages } from '../../utils/reportImageCapture'

function groupReportsByPeriod(reports) {
  const map = new Map()
  for (const r of reports) {
    const key = `${r.periodStart}|${r.periodEnd}|${r.periodLabel || r.testTitle || ''}`
    if (!map.has(key)) {
      map.set(key, {
        periodStart: r.periodStart,
        periodEnd: r.periodEnd,
        periodLabel: r.periodLabel || r.testTitle || '성실도 보고서',
        reports: [],
      })
    }
    map.get(key).reports.push(r)
  }
  return [...map.values()].sort(
    (a, b) => new Date(b.periodEnd).getTime() - new Date(a.periodEnd).getTime(),
  )
}

function archiveZipFilename(periodLabel) {
  const safe = (periodLabel || 'reports').replace(/[\\/:*?"<>|]/g, '_').trim()
  return `${safe || 'reports'}.zip`
}

function toDateInputValue(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export default function ClassReportsSection({ classId, canManage, isStudent, onError }) {
  const [reports, setReports] = useState([])
  const [presets, setPresets] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [expandedKeys, setExpandedKeys] = useState(() => new Set())
  const [downloadingKey, setDownloadingKey] = useState('')
  const [showGenerate, setShowGenerate] = useState(false)
  const [generateForm, setGenerateForm] = useState({
    periodStart: '',
    periodEnd: '',
    presetId: '',
  })
  const [studentTarget, setStudentTarget] = useState(() => createInitialTarget(true))
  const [presetForm, setPresetForm] = useState({ name: '', periodStart: '', periodEnd: '' })
  const [editingPresetId, setEditingPresetId] = useState(null)

  const reportGroups = useMemo(() => groupReportsByPeriod(reports), [reports])

  const load = useCallback(async () => {
    setLoading(true)
    onError('')
    try {
      const list = await fetchClassReports(classId)
      setReports(list)
      if (canManage) {
        setPresets(await fetchReportPeriodPresets(classId))
      } else {
        setPresets([])
      }
    } catch (err) {
      onError(err.message)
      setReports([])
      setPresets([])
    } finally {
      setLoading(false)
    }
  }, [classId, canManage, onError])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (reportGroups.length === 0) return
    setExpandedKeys((prev) => {
      if (prev.size > 0) return prev
      const g = reportGroups[0]
      return new Set([`${g.periodStart}|${g.periodEnd}|${g.periodLabel}`])
    })
  }, [reportGroups])

  function groupKey(group) {
    return `${group.periodStart}|${group.periodEnd}|${group.periodLabel}`
  }

  function toggleGroup(key) {
    setExpandedKeys((prev) => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
  }

  function applyPreset(presetId) {
    const preset = presets.find((p) => String(p.presetId) === presetId)
    if (!preset) {
      setGenerateForm((f) => ({ ...f, presetId: '' }))
      return
    }
    setGenerateForm({
      periodStart: preset.periodStart,
      periodEnd: preset.periodEnd,
      presetId: String(preset.presetId),
    })
  }

  async function handleArchiveDownload(group) {
    const key = groupKey(group)
    setDownloadingKey(key)
    onError('')
    try {
      await downloadPeriodReportsArchive(
        classId,
        toDateInputValue(group.periodStart),
        toDateInputValue(group.periodEnd),
        archiveZipFilename(group.periodLabel) + '.zip',
      )
    } catch (err) {
      onError(err.message)
    } finally {
      setDownloadingKey('')
    }
  }

  async function handleGenerate(e) {
    e.preventDefault()
    if (!generateForm.periodStart || !generateForm.periodEnd) return
    if (studentTarget.mode === 'custom' && studentTarget.studentIds.length === 0) {
      onError('학생을 한 명 이상 선택하세요.')
      return
    }
    if (
      !window.confirm(
        '선택한 기간·학생에 대해 보고서를 생성합니다. 같은 기간의 기존 보고서는 덮어씁니다. 계속할까요?',
      )
    ) {
      return
    }
    setSubmitting(true)
    onError('')
    try {
      const resolvedIds =
        studentTarget.mode === 'all'
          ? (await fetchStudyRecordStudents(classId)).map((s) => s.studentId)
          : studentTarget.studentIds
      const body = {
        periodStart: generateForm.periodStart,
        periodEnd: generateForm.periodEnd,
        studentIds: resolvedIds,
        presetId: generateForm.presetId ? Number(generateForm.presetId) : null,
      }
      const result = await generateReports(classId, body)
      if (result.reportIds?.length) {
        await captureAndUploadReportImages(result.reportIds)
      }
      setShowGenerate(false)
      await load()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleSavePreset(e) {
    e.preventDefault()
    if (!presetForm.name.trim() || !presetForm.periodStart || !presetForm.periodEnd) return
    setSubmitting(true)
    onError('')
    try {
      const body = {
        name: presetForm.name.trim(),
        periodStart: presetForm.periodStart,
        periodEnd: presetForm.periodEnd,
      }
      if (editingPresetId) {
        await updateReportPeriodPreset(classId, editingPresetId, body)
      } else {
        await createReportPeriodPreset(classId, body)
      }
      setPresetForm({ name: '', periodStart: '', periodEnd: '' })
      setEditingPresetId(null)
      await load()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDeletePreset(presetId) {
    if (!window.confirm('이 기간 프리셋을 삭제할까요?')) return
    onError('')
    try {
      await deleteReportPeriodPreset(classId, presetId)
      await load()
    } catch (err) {
      onError(err.message)
    }
  }

  if (loading) {
    return <p className="ams-class-detail__empty">불러오는 중…</p>
  }

  const hasReports = reports.length > 0

  return (
    <section className="ams-reports">
      {canManage && (
        <>
          <div className="ams-reports-presets ams-card ams-card--elevated">
            <h3 className="ams-class-detail__heading">기간 프리셋</h3>
            <p className="ams-class-detail__hint-inline">
              자주 쓰는 보고 기간을 저장해 두고 생성 시 불러올 수 있습니다.
            </p>
            <form className="ams-reports-preset-form" onSubmit={handleSavePreset}>
              <label className="ams-field ams-field--compact">
                <span className="ams-field__label">이름</span>
                <input
                  className="ams-field__input"
                  value={presetForm.name}
                  onChange={(e) => setPresetForm({ ...presetForm, name: e.target.value })}
                  placeholder="예: 1학기 기말"
                  maxLength={100}
                  required
                />
              </label>
              <label className="ams-field ams-field--compact">
                <span className="ams-field__label">시작일</span>
                <input
                  className="ams-field__input"
                  type="date"
                  value={presetForm.periodStart}
                  onChange={(e) => setPresetForm({ ...presetForm, periodStart: e.target.value })}
                  required
                />
              </label>
              <label className="ams-field ams-field--compact">
                <span className="ams-field__label">종료일</span>
                <input
                  className="ams-field__input"
                  type="date"
                  value={presetForm.periodEnd}
                  onChange={(e) => setPresetForm({ ...presetForm, periodEnd: e.target.value })}
                  required
                />
              </label>
              <div className="ams-reports-preset-form__actions">
                {editingPresetId && (
                  <button
                    type="button"
                    className="ams-btn ams-btn--ghost ams-btn--sm"
                    onClick={() => {
                      setEditingPresetId(null)
                      setPresetForm({ name: '', periodStart: '', periodEnd: '' })
                    }}
                  >
                    취소
                  </button>
                )}
                <button type="submit" className="ams-btn ams-btn--primary ams-btn--sm" disabled={submitting}>
                  {editingPresetId ? '수정' : '프리셋 추가'}
                </button>
              </div>
            </form>
            {presets.length > 0 ? (
              <ul className="ams-reports-preset-list">
                {presets.map((p) => (
                  <li key={p.presetId} className="ams-reports-preset-list__item">
                    <span>
                      <strong>{p.name}</strong>
                      <span className="ams-reports-preset-list__dates">
                        {formatLocalDate(p.periodStart)} ~ {formatLocalDate(p.periodEnd)}
                      </span>
                    </span>
                    <span className="ams-reports-preset-list__actions">
                      <button
                        type="button"
                        className="ams-btn ams-btn--ghost ams-btn--sm"
                        onClick={() => {
                          setEditingPresetId(p.presetId)
                          setPresetForm({
                            name: p.name,
                            periodStart: p.periodStart,
                            periodEnd: p.periodEnd,
                          })
                        }}
                      >
                        수정
                      </button>
                      <button
                        type="button"
                        className="ams-btn ams-btn--ghost ams-btn--sm"
                        onClick={() => handleDeletePreset(p.presetId)}
                      >
                        삭제
                      </button>
                    </span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="ams-class-detail__empty">등록된 프리셋이 없습니다.</p>
            )}
          </div>

          <div className="ams-reports-generate">
            <div className="ams-reports-generate__head">
              <h3 className="ams-class-detail__heading">보고서 생성</h3>
              <button
                type="button"
                className={`ams-btn ams-btn--sm${showGenerate ? ' ams-btn--ghost' : ' ams-btn--primary'}`}
                onClick={() => setShowGenerate((v) => !v)}
              >
                {showGenerate ? '닫기' : '+ 보고서 생성'}
              </button>
            </div>
            {showGenerate && (
              <form className="ams-reports-generate__form ams-card ams-card--elevated" onSubmit={handleGenerate}>
                <p className="ams-class-detail__hint-inline">
                  설정한 기간 안의 숙제·클리닉·영상·완료된 테스트를 모두 집계해 학생별 PNG 보고서를 만듭니다.
                </p>
                <div className="ams-reports-generate__grid">
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">프리셋 불러오기</span>
                    <select
                      className="ams-field__input"
                      value={generateForm.presetId}
                      onChange={(e) => applyPreset(e.target.value)}
                    >
                      <option value="">직접 입력</option>
                      {presets.map((p) => (
                        <option key={p.presetId} value={p.presetId}>
                          {p.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">시작일</span>
                    <input
                      className="ams-field__input"
                      type="date"
                      value={generateForm.periodStart}
                      onChange={(e) =>
                        setGenerateForm({ ...generateForm, periodStart: e.target.value, presetId: '' })
                      }
                      required
                    />
                  </label>
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">종료일</span>
                    <input
                      className="ams-field__input"
                      type="date"
                      value={generateForm.periodEnd}
                      onChange={(e) =>
                        setGenerateForm({ ...generateForm, periodEnd: e.target.value, presetId: '' })
                      }
                      required
                    />
                  </label>
                </div>
                <StudentTargetPicker
                  classId={classId}
                  allByDefault
                  value={studentTarget}
                  onChange={setStudentTarget}
                  disabled={submitting}
                  label="보고서 대상 학생"
                />
                <div className="ams-reports-generate__foot">
                  <button type="submit" className="ams-btn ams-btn--primary ams-btn--sm" disabled={submitting}>
                    {submitting ? '생성·PNG 저장 중…' : '보고서 생성'}
                  </button>
                </div>
              </form>
            )}
          </div>
        </>
      )}

      <h3 className="ams-class-detail__heading">성실도 보고서</h3>

      {!hasReports ? (
        <p className="ams-class-detail__empty">
          {canManage
            ? '생성된 보고서가 없습니다. 기간과 학생을 선택해 보고서를 생성하세요.'
            : '생성된 성실도 보고서가 없습니다.'}
        </p>
      ) : (
        <ul className="ams-reports-groups">
          {reportGroups.map((group) => {
            const key = groupKey(group)
            const expanded = expandedKeys.has(key)
            return (
              <li key={key} className="ams-reports-group">
                <div className="ams-reports-group__header">
                  <button
                    type="button"
                    className="ams-reports-group__toggle"
                    aria-expanded={expanded}
                    onClick={() => toggleGroup(key)}
                  >
                    <span
                      className={`ams-reports-group__chevron${expanded ? ' ams-reports-group__chevron--open' : ''}`}
                      aria-hidden
                    >
                      ▶
                    </span>
                    <span className="ams-reports-group__title-wrap">
                      <strong className="ams-reports-group__title">{group.periodLabel}</strong>
                      <span className="ams-reports-group__meta">
                        {formatReportPeriod(group.periodStart, group.periodEnd)} · 보고서{' '}
                        {group.reports.length}명
                      </span>
                    </span>
                  </button>
                  {canManage && (
                    <button
                      type="button"
                      className="ams-btn ams-btn--ghost ams-btn--sm"
                      disabled={downloadingKey !== ''}
                      onClick={() => handleArchiveDownload(group)}
                    >
                      {downloadingKey === key ? '다운로드 중…' : 'PNG ZIP'}
                    </button>
                  )}
                </div>
                {expanded && (
                  <ul className="ams-reports__list ams-reports__list--nested">
                    {group.reports.map((r) => (
                      <li key={r.reportId}>
                        <button
                          type="button"
                          className="ams-reports__item"
                          onClick={() => setSelectedId(r.reportId)}
                        >
                          {!isStudent && <strong>{r.studentName}</strong>}
                          {isStudent && <strong>{r.periodLabel || r.testTitle}</strong>}
                          <span className="ams-reports__score">
                            종합 {r.totalScore}점 · {r.overallGrade}
                          </span>
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </li>
            )
          })}
        </ul>
      )}

      {selectedId && (
        <div className="ams-report-modal-backdrop" onClick={() => setSelectedId(null)}>
          <div onClick={(e) => e.stopPropagation()}>
            <ReportDetailModal
              reportId={selectedId}
              canManage={canManage}
              onClose={() => setSelectedId(null)}
              onError={onError}
            />
          </div>
        </div>
      )}
    </section>
  )
}
