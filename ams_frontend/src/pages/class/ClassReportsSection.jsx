import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  downloadReportPdf,
  downloadTestReportsArchive,
  fetchClassReports,
  fetchReportDetail,
  fetchReportGenerationTargets,
  formatReportPeriod,
  generateClassReports,
  updateReportComment,
} from '../../api/reportsApi'

function groupReportsByTest(reports, targets) {
  const testAtById = Object.fromEntries(
    (targets || []).map((t) => [String(t.testId), t.testAt]),
  )
  const map = new Map()
  for (const r of reports) {
    if (!map.has(r.testId)) {
      map.set(r.testId, {
        testId: r.testId,
        testTitle: r.testTitle,
        testAt: testAtById[String(r.testId)] ?? r.createdAt,
        reports: [],
      })
    }
    map.get(r.testId).reports.push(r)
  }
  return [...map.values()].sort(
    (a, b) => new Date(b.testAt).getTime() - new Date(a.testAt).getTime(),
  )
}

function archiveZipFilename(testTitle) {
  const safe = (testTitle || 'reports').replace(/[\\/:*?"<>|]/g, '_').trim()
  return `${safe || 'reports'}.zip`
}

function MetricBar({ label, rate }) {
  const value = rate ?? 0
  const tone = value < 60 ? 'low' : 'normal'
  return (
    <li className={`ams-report-modal__metric ams-report-modal__metric--${tone}`}>
      <span className="ams-report-modal__metric-label">{label}</span>
      <span
        className="ams-report-modal__metric-bar"
        role="progressbar"
        aria-valuenow={value}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={`${label} ${value}%`}
      >
        <span
          className="ams-report-modal__metric-fill"
          style={{ width: `${Math.min(100, value)}%` }}
        />
      </span>
      <span className="ams-report-modal__metric-pct">
        {rate != null ? `${rate}%` : '—'}
      </span>
    </li>
  )
}

/** 테스트 막대: 종합 환산과 동일한 0~100 스케일, 표기는 점수(점) */
function TestScoreMetricBar({ rawScore }) {
  if (rawScore == null) {
    return (
      <li className="ams-report-modal__metric">
        <span className="ams-report-modal__metric-label">테스트</span>
        <span className="ams-report-modal__metric-bar" aria-hidden />
        <span className="ams-report-modal__metric-pct">—</span>
      </li>
    )
  }
  const n = Number(rawScore)
  const bar = Number.isNaN(n) ? 0 : Math.max(0, Math.min(100, Math.round(n)))
  const tone = bar < 60 ? 'low' : 'normal'
  const label = Number.isNaN(n)
    ? '—'
    : `${Number.isInteger(n) ? n : parseFloat(n.toFixed(1))}점`
  return (
    <li className={`ams-report-modal__metric ams-report-modal__metric--${tone}`}>
      <span className="ams-report-modal__metric-label">테스트</span>
      <span
        className="ams-report-modal__metric-bar"
        role="progressbar"
        aria-valuenow={bar}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={`테스트 ${label}`}
      >
        <span
          className="ams-report-modal__metric-fill"
          style={{ width: `${bar}%` }}
        />
      </span>
      <span className="ams-report-modal__metric-pct">{label}</span>
    </li>
  )
}

function formatTestWhen(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('ko-KR', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function ReportDetailModal({ reportId, canManage, onClose, onError }) {
  const [detail, setDetail] = useState(null)
  const [comment, setComment] = useState('')
  const [saving, setSaving] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    ;(async () => {
      setLoading(true)
      onError('')
      try {
        const d = await fetchReportDetail(reportId)
        setDetail(d)
        setComment(d.teacherComment || '')
      } catch (err) {
        onError(err.message)
      } finally {
        setLoading(false)
      }
    })()
  }, [reportId, onError])

  async function handleSaveComment() {
    setSaving(true)
    onError('')
    try {
      const updated = await updateReportComment(reportId, comment)
      setDetail(updated)
    } catch (err) {
      onError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handlePdf() {
    onError('')
    try {
      await downloadReportPdf(reportId)
    } catch (err) {
      onError(err.message)
    }
  }

  if (loading) {
    return (
      <div className="ams-report-modal">
        <p>불러오는 중…</p>
      </div>
    )
  }

  if (!detail) return null

  const period = formatReportPeriod(detail.periodStart, detail.periodEnd)

  return (
    <div className="ams-report-modal" role="dialog" aria-modal="true" aria-label="성실도 보고서">
      <header className="ams-report-modal__header">
        <button
          type="button"
          className="ams-icon-btn"
          onClick={onClose}
          aria-label="닫기"
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden
          >
            <path d="M15 5 8 12l7 7" />
          </svg>
        </button>
        <h3 className="ams-report-modal__title">성실도 보고서</h3>
        <button
          type="button"
          className="ams-icon-btn"
          onClick={handlePdf}
          aria-label="PDF 다운로드"
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden
          >
            <path d="M12 4v11" />
            <path d="m7 10 5 5 5-5" />
            <path d="M5 20h14" />
          </svg>
        </button>
      </header>

      <section className="ams-report-modal__hero">
        <p className="ams-report-modal__hero-meta">
          {period} · {detail.testTitle}
        </p>
        <p className="ams-report-modal__hero-score">
          <strong>{detail.totalScore}점</strong>
          <span>{detail.overallGrade}등급</span>
        </p>
        <ul className="ams-report-modal__metrics">
          <MetricBar label="숙제" rate={detail.homeworkRate} />
          <MetricBar label="클리닉" rate={detail.clinicRate} />
          <TestScoreMetricBar rawScore={detail.testRawScore} />
          <MetricBar label="영상인증" rate={detail.videoRate} />
        </ul>
      </section>

      {canManage ? (
        <div className="ams-report-modal__comment">
          <label htmlFor="ams-report-comment">담임 코멘트</label>
          <textarea
            id="ams-report-comment"
            rows={3}
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="학생·학부모에게 전달할 코멘트"
          />
          <button
            type="button"
            className="ams-btn ams-btn--primary ams-btn--sm"
            disabled={saving}
            onClick={handleSaveComment}
          >
            {saving ? '저장 중…' : '코멘트 저장'}
          </button>
        </div>
      ) : (
        detail.teacherComment && (
          <div className="ams-report-modal__comment-card">
            <p className="ams-report-modal__comment-label">담임 코멘트</p>
            <p className="ams-report-modal__comment-body">{detail.teacherComment}</p>
          </div>
        )
      )}

      <ul className="ams-report-modal__stats">
        <li className="ams-report-modal__stat">
          <strong>
            {detail.homeworkTotal > 0
              ? `${detail.homeworkSubmitted}/${detail.homeworkTotal}`
              : '—'}
          </strong>
          <span>숙제 제출</span>
        </li>
        <li className="ams-report-modal__stat">
          <strong>
            {detail.clinicTotal > 0
              ? `${detail.clinicAttended}/${detail.clinicTotal}`
              : '—'}
          </strong>
          <span>클리닉</span>
        </li>
        <li className="ams-report-modal__stat ams-report-modal__stat--accent">
          <strong>{detail.testRawScore ?? '—'}</strong>
          <span>
            점수
            {detail.testUpperRankPct != null ? ` · 상위 ${detail.testUpperRankPct}%` : ''}
          </span>
        </li>
      </ul>

      <p className="ams-report-modal__note">
        종합 점수는 숙제·클리닉(%), 테스트(원점수·만점 100 기준)를 40·30·30으로 합산합니다. 하단
        점수 카드의 반 평균·상위 %는 비교용이며 영상 인증은 종합에 포함되지 않습니다.
      </p>
    </div>
  )
}

export default function ClassReportsSection({ classId, canManage, isStudent, onError }) {
  const [reports, setReports] = useState([])
  const [targets, setTargets] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [generatingTestId, setGeneratingTestId] = useState(null)
  const [expandedTestIds, setExpandedTestIds] = useState(() => new Set())
  const [downloadingArchiveTestId, setDownloadingArchiveTestId] = useState(null)

  const reportGroups = useMemo(
    () => groupReportsByTest(reports, targets),
    [reports, targets],
  )

  const load = useCallback(async () => {
    setLoading(true)
    onError('')
    try {
      const list = await fetchClassReports(classId)
      setReports(list)
      if (canManage) {
        setTargets(await fetchReportGenerationTargets(classId))
      } else {
        setTargets([])
      }
    } catch (err) {
      onError(err.message)
      setReports([])
      setTargets([])
    } finally {
      setLoading(false)
    }
  }, [classId, canManage, onError])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (reportGroups.length === 0) {
      return
    }
    setExpandedTestIds((prev) => {
      if (prev.size > 0) {
        return prev
      }
      return new Set([reportGroups[0].testId])
    })
  }, [reportGroups])

  function toggleTestGroup(testId) {
    setExpandedTestIds((prev) => {
      const next = new Set(prev)
      if (next.has(testId)) {
        next.delete(testId)
      } else {
        next.add(testId)
      }
      return next
    })
  }

  async function handleArchiveDownload(group) {
    setDownloadingArchiveTestId(group.testId)
    onError('')
    try {
      await downloadTestReportsArchive(classId, group.testId, archiveZipFilename(group.testTitle))
    } catch (err) {
      onError(err.message)
    } finally {
      setDownloadingArchiveTestId(null)
    }
  }

  async function handleGenerate(testId, reportGenerated) {
    if (
      reportGenerated &&
      !window.confirm(
        '기존 보고서를 삭제하고 다시 만듭니다. PDF·집계 수치가 갱신되며 학생 알림이 다시 발송될 수 있습니다. 계속할까요?',
      )
    ) {
      return
    }
    setGeneratingTestId(testId)
    onError('')
    try {
      await generateClassReports(classId, testId)
      await load()
    } catch (err) {
      onError(err.message)
    } finally {
      setGeneratingTestId(null)
    }
  }

  if (loading) {
    return <p className="ams-class-detail__empty">불러오는 중…</p>
  }

  const pendingTargets = targets.filter((t) => !t.reportGenerated)
  const hasReports = reports.length > 0

  return (
    <section className="ams-reports">
      {canManage && targets.length > 0 && (
        <div className="ams-reports-generate">
          <h3 className="ams-class-detail__heading">보고서 생성</h3>
          <p className="ams-class-detail__hint-inline">
            테스트 점수 저장(시험 완료) 후, 숙제·클리닉·영상 정리가 끝난 뒤 생성하세요. 시험 예정
            시각 이후에 생성하면 집계에 유리합니다.
          </p>
          <ul className="ams-reports-generate__list">
            {targets.map((t) => (
              <li key={t.testId} className="ams-reports-generate__item">
                <div className="ams-reports-generate__body">
                  <strong>{t.title}</strong>
                  <span className="ams-reports-generate__meta">
                    시험 {formatTestWhen(t.testAt)}
                    {t.completedAt ? ` · 채점 ${formatTestWhen(t.completedAt)}` : ''}
                  </span>
                  {t.reportGenerated ? (
                    <span className="ams-reports-generate__badge">생성됨</span>
                  ) : (
                    <span className="ams-reports-generate__badge ams-reports-generate__badge--pending">
                      미생성
                    </span>
                  )}
                </div>
                <button
                  type="button"
                  className={
                    t.reportGenerated
                      ? 'ams-btn ams-btn--ghost ams-btn--sm'
                      : 'ams-btn ams-btn--primary ams-btn--sm'
                  }
                  disabled={generatingTestId != null}
                  onClick={() => handleGenerate(t.testId, t.reportGenerated)}
                >
                  {generatingTestId === t.testId
                    ? '처리 중…'
                    : t.reportGenerated
                      ? '재생성'
                      : '보고서 생성'}
                </button>
              </li>
            ))}
          </ul>
          {pendingTargets.length > 0 && (
            <p className="ams-reports-generate__note">
              미생성 테스트 {pendingTargets.length}건
            </p>
          )}
        </div>
      )}

      <h3 className="ams-class-detail__heading">
        {canManage && targets.length > 0 ? '생성된 보고서' : '성실도 보고서'}
      </h3>

      {!hasReports ? (
        <p className="ams-class-detail__empty">
          {canManage
            ? '생성된 보고서가 없습니다. 완료된 테스트가 있으면 위에서 보고서를 생성할 수 있습니다.'
            : '생성된 성실도 보고서가 없습니다.'}
        </p>
      ) : (
        <ul className="ams-reports-groups">
          {reportGroups.map((group) => {
            const expanded = expandedTestIds.has(group.testId)
            return (
              <li key={group.testId} className="ams-reports-group">
                <div className="ams-reports-group__header">
                  <button
                    type="button"
                    className="ams-reports-group__toggle"
                    aria-expanded={expanded}
                    onClick={() => toggleTestGroup(group.testId)}
                  >
                    <span
                      className={`ams-reports-group__chevron${expanded ? ' ams-reports-group__chevron--open' : ''}`}
                      aria-hidden
                    >
                      ▶
                    </span>
                    <span className="ams-reports-group__title-wrap">
                      <strong className="ams-reports-group__title">{group.testTitle}</strong>
                      <span className="ams-reports-group__meta">
                        {group.testAt ? `시험 ${formatTestWhen(group.testAt)} · ` : ''}
                        보고서 {group.reports.length}명
                      </span>
                    </span>
                  </button>
                  {canManage && (
                    <button
                      type="button"
                      className="ams-btn ams-btn--ghost ams-btn--sm"
                      disabled={downloadingArchiveTestId != null}
                      onClick={() => handleArchiveDownload(group)}
                    >
                      {downloadingArchiveTestId === group.testId ? '다운로드 중…' : 'PDF 전체'}
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
                          {isStudent && <strong>{r.testTitle}</strong>}
                          <span>{formatReportPeriod(r.periodStart, r.periodEnd)}</span>
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
