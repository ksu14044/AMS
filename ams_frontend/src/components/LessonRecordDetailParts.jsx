import { Link } from 'react-router-dom'
import { dayLabel } from '../auth/dayLabels'
import { formatTargetSummary, formatVideoCertTargetSummary } from '../utils/assignmentTargets'

const JS_DAY_TO_CLINIC = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']

const LINKED_META = {
  homework: { label: '숙제', tone: 'homework' },
  test: { label: '테스트', tone: 'test' },
  video: { label: '영상', tone: 'video' },
  clinic: { label: '클리닉', tone: 'clinic' },
}

function dayOfWeekFromDate(dateStr) {
  if (!dateStr) return 'MON'
  const d = new Date(`${dateStr}T12:00:00`)
  return JS_DAY_TO_CLINIC[d.getDay()]
}

export function formatLessonDate(dateStr) {
  if (!dateStr) return '—'
  const [y, m, d] = dateStr.split('-').map(Number)
  return `${y}.${String(m).padStart(2, '0')}.${String(d).padStart(2, '0')}`
}

export function formatLessonDateWithWeekday(dateStr) {
  if (!dateStr) return '—'
  const dow = dayLabel(dayOfWeekFromDate(dateStr))
  return `${formatLessonDate(dateStr)} (${dow})`
}

export function formatDateTime(iso) {
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

export function lessonRecordShellTitle(detail) {
  const summary = detail?.summary?.trim()
  if (summary) {
    const line = summary.split('\n')[0]
    return line.length > 52 ? `${line.slice(0, 52)}…` : line
  }
  return formatLessonDate(detail?.lessonDate)
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

export function linkedItemHref(classId, item) {
  if (item.type === 'homework') return `/classes/${classId}/homeworks/${item.id}`
  if (item.type === 'test') return `/classes/${classId}/tests/${item.id}`
  return null
}

export function linkedItemSubtitle(item) {
  if (item.type === 'homework') {
    const parts = []
    if (item.questionCount) parts.push(`${item.questionCount}문항`)
    if (item.targets) parts.push(`대상 ${formatTargetSummary(item.targets)}`)
    return parts.join(' · ') || null
  }
  if (item.type === 'test') {
    const parts = []
    if (item.questionCount) parts.push(`${item.questionCount}문항`)
    if (item.retakeThresholdCount != null) parts.push(`합격 ${item.retakeThresholdCount}문항 이상`)
    if (item.targets) parts.push(`대상 ${formatTargetSummary(item.targets)}`)
    return parts.join(' · ') || null
  }
  if (item.type === 'video') {
    const parts = []
    if (item.targets) parts.push(formatVideoCertTargetSummary(item.targets))
    return parts.join(' · ') || null
  }
  if (item.type === 'clinic') {
    const parts = []
    if (item.clinicDate) parts.push(formatLessonDate(item.clinicDate))
    if (item.clinicStartTime) parts.push(item.clinicStartTime)
    if (item.presetName) parts.push(item.presetName)
    if (item.maxCapacity) parts.push(`정원 ${item.maxCapacity}명`)
    if (item.targets) parts.push(`대상 ${formatTargetSummary(item.targets)}`)
    return parts.join(' · ') || null
  }
  return null
}

function LinkedBadgeGroup({ record }) {
  const badges = linkedBadges(record)
  if (badges.length === 0) {
    return <span className="ams-lesson-record-detail__chip ams-lesson-record-detail__chip--empty">귀속 항목 없음</span>
  }
  return (
    <div className="ams-lesson-record-detail__chips">
      {badges.map((b) => (
        <span
          key={b.key}
          className={`ams-lesson-record-detail__chip ams-lesson-record-detail__chip--${b.key}`}
        >
          {b.label}
          {b.count > 1 ? ` ${b.count}` : ''}
        </span>
      ))}
    </div>
  )
}

export function LessonRecordDetailHero({ detail }) {
  return (
    <header className="ams-lesson-record-detail__hero">
      <div className="ams-lesson-record-detail__hero-main">
        <p className="ams-lesson-record-detail__hero-label">수업일</p>
        <time className="ams-lesson-record-detail__date" dateTime={detail.lessonDate}>
          {formatLessonDateWithWeekday(detail.lessonDate)}
        </time>
        <p className="ams-lesson-record-detail__meta">
          <span>{detail.authorName}</span>
          <span aria-hidden>·</span>
          <span>등록 {formatDateTime(detail.createdAt)}</span>
        </p>
      </div>
      <LinkedBadgeGroup record={detail} />
    </header>
  )
}

export function LessonRecordSummarySection({
  detail,
  canEdit,
  editSummary,
  onEditSummaryChange,
  onSubmit,
  submitting,
}) {
  return (
    <section
      className="ams-lesson-record-detail__section ams-lesson-record-detail__section--summary"
      aria-labelledby="lesson-summary-heading"
    >
      <h2 id="lesson-summary-heading" className="ams-lesson-record-detail__section-title">
        수업 요약
      </h2>
      {canEdit ? (
        <form className="ams-lesson-record-detail__summary-form" onSubmit={onSubmit}>
          <label className="ams-lesson-record-detail__summary-label" htmlFor="lesson-summary-input">
            수업 내용
          </label>
          <textarea
            id="lesson-summary-input"
            className="ams-field__textarea ams-lesson-record-detail__summary-input"
            value={editSummary}
            onChange={(e) => onEditSummaryChange(e.target.value)}
            rows={8}
            placeholder="오늘 수업에서 다룬 내용, 다음 수업 안내 등"
            required
          />
          <div className="ams-lesson-record-detail__summary-foot">
            <span className="ams-lesson-record-detail__char-count">{editSummary.length}자</span>
            <button type="submit" className="ams-btn ams-btn--primary ams-btn--sm" disabled={submitting}>
              {submitting ? '저장 중…' : '요약 저장'}
            </button>
          </div>
        </form>
      ) : (
        <div className="ams-lesson-record-detail__summary-read">
          {detail.summary?.trim() ? (
            <p className="ams-lesson-record-detail__summary-body">{detail.summary}</p>
          ) : (
            <p className="ams-lesson-record-detail__summary-empty">등록된 요약이 없습니다.</p>
          )}
        </div>
      )}
    </section>
  )
}

export function LessonRecordLinkedCard({
  item,
  classId,
  canEdit,
  isEditing,
  submitting,
  onEdit,
  onDelete,
  editForm,
}) {
  const tone = LINKED_META[item.type]?.tone ?? item.type
  const href = linkedItemHref(classId, item)
  const subtitle = linkedItemSubtitle(item)

  return (
    <li
      className={`ams-lesson-record-detail__linked-card ams-lesson-record-detail__linked-card--${tone}${isEditing ? ' ams-lesson-record-detail__linked-card--editing' : ''}`}
    >
      <div className="ams-lesson-record-detail__linked-card-inner">
        <div className="ams-lesson-record-detail__linked-main">
          <span className={`ams-lesson-record-detail__linked-type ams-lesson-record-detail__linked-type--${tone}`}>
            {linkedTypeLabel(item.type)}
          </span>
          <div className="ams-lesson-record-detail__linked-text">
            {href ? (
              <Link to={href} className="ams-lesson-record-detail__linked-title">
                {item.title}
              </Link>
            ) : (
              <span className="ams-lesson-record-detail__linked-title ams-lesson-record-detail__linked-title--plain">
                {item.title}
              </span>
            )}
            {subtitle ? <p className="ams-lesson-record-detail__linked-sub">{subtitle}</p> : null}
            {item.type === 'video' && item.youtubeUrl ? (
              <a
                href={item.youtubeUrl}
                className="ams-lesson-record-detail__linked-link"
                target="_blank"
                rel="noopener noreferrer"
              >
                YouTube 열기
              </a>
            ) : null}
            {href ? (
              <Link to={href} className="ams-lesson-record-detail__linked-go">
                {item.type === 'homework' ? '숙제 확인' : '테스트 확인'} →
              </Link>
            ) : null}
          </div>
        </div>
        {canEdit && (
          <div className="ams-lesson-record-detail__linked-actions">
            {item.canEdit && (
              <button
                type="button"
                className="ams-btn ams-btn--ghost ams-btn--sm"
                onClick={onEdit}
                disabled={submitting}
              >
                {isEditing ? '닫기' : '수정'}
              </button>
            )}
            {item.canDelete && (
              <button
                type="button"
                className="ams-btn ams-btn--ghost ams-btn--sm ams-lesson-record-detail__linked-delete"
                onClick={onDelete}
                disabled={submitting}
              >
                삭제
              </button>
            )}
          </div>
        )}
      </div>
      {isEditing && editForm ? (
        <div className="ams-lesson-record-detail__linked-edit">{editForm}</div>
      ) : null}
    </li>
  )
}

export function LessonRecordLinkedEmpty({ canEdit }) {
  return (
    <div className="ams-lesson-record-detail__linked-empty">
      <p className="ams-lesson-record-detail__linked-empty-title">연결된 항목이 없습니다</p>
      <p className="ams-lesson-record-detail__linked-empty-desc">
        {canEdit
          ? '「항목 추가」로 이 수업에 숙제·테스트·영상·클리닉을 연결할 수 있습니다.'
          : '담당 교직원이 항목을 추가하면 여기에 표시됩니다.'}
      </p>
    </div>
  )
}
