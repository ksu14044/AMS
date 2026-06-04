import { formatReportPeriod } from '../api/reportsApi'

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
      <span className="ams-report-modal__metric-pct">{rate != null ? `${rate}%` : '—'}</span>
    </li>
  )
}

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

/**
 * 성실도 보고서 모달 본문 — 화면·PNG 캡처 공통
 * @param {{ detail: object, captureMode?: boolean, commentEdit?: { value: string, onChange: (v: string) => void, onSave: () => void, saving: boolean, pngSyncing?: boolean } }} props
 */
export default function ReportDetailContent({ detail, captureMode = false, commentEdit }) {
  if (!detail) return null

  const period = formatReportPeriod(detail.periodStart, detail.periodEnd)
  const subtitle = [detail.studentName, detail.className].filter(Boolean).join(' · ')

  return (
    <div
      className={
        captureMode ? 'ams-report-modal ams-report-modal--capture' : 'ams-report-modal__body'
      }
      role={captureMode ? undefined : 'document'}
      aria-label="성실도 보고서"
    >
      {captureMode && (
        <header className="ams-report-modal__header ams-report-modal__header--capture">
          <h3 className="ams-report-modal__title">성실도 보고서</h3>
          {subtitle ? <p className="ams-report-modal__subtitle">{subtitle}</p> : null}
        </header>
      )}

      <section className="ams-report-modal__hero">
        <p className="ams-report-modal__hero-meta">
          {period}
          {detail.periodLabel || detail.testTitle
            ? ` · ${detail.periodLabel || detail.testTitle}`
            : ''}
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

      {commentEdit ? (
        <div className="ams-report-modal__comment-card ams-report-modal__comment-card--edit">
          <label className="ams-report-modal__comment-label" htmlFor="ams-report-comment-input">
            담임 코멘트
          </label>
          <textarea
            id="ams-report-comment-input"
            className="ams-report-modal__comment-input"
            rows={3}
            value={commentEdit.value}
            onChange={(e) => commentEdit.onChange(e.target.value)}
            placeholder="학생·학부모에게 전달할 코멘트"
            disabled={commentEdit.saving}
          />
          <button
            type="button"
            className="ams-btn ams-btn--primary ams-btn--sm"
            disabled={commentEdit.saving}
            onClick={commentEdit.onSave}
          >
            {commentEdit.saving
              ? '저장 중…'
              : commentEdit.pngSyncing
                ? 'PNG 반영 중…'
                : '저장'}
          </button>
          {commentEdit.pngSyncing && !commentEdit.saving ? (
            <p className="ams-report-modal__comment-hint">PNG는 백그라운드에서 반영됩니다.</p>
          ) : null}
        </div>
      ) : detail.teacherComment ? (
        <div className="ams-report-modal__comment-card">
          <p className="ams-report-modal__comment-label">담임 코멘트</p>
          <p className="ams-report-modal__comment-body">{detail.teacherComment}</p>
        </div>
      ) : null}

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
            {detail.clinicTotal > 0 ? `${detail.clinicAttended}/${detail.clinicTotal}` : '—'}
          </strong>
          <span>클리닉</span>
        </li>
        <li className="ams-report-modal__stat ams-report-modal__stat--accent">
          <strong>{detail.testRawScore ?? '—'}</strong>
          <span>
            점수
            {detail.testRank != null
              ? ` · ${detail.testRank}등`
              : detail.testUpperRankPct != null
                ? ` · 상위 ${detail.testUpperRankPct}%`
                : ''}
          </span>
        </li>
      </ul>

      <p className="ams-report-modal__note">
        종합 점수는 기간 내 숙제·클리닉(%), 완료 시험 점수(루트별 최신 재시험 % 평균)를 40·30·30으로
        합산합니다. 테스트 석차·반 평균은 기간 내 가장 최근 시험 기준이며, 영상 인증은 종합에 포함되지
        않습니다.
      </p>
    </div>
  )
}
