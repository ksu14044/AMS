import { Link } from 'react-router-dom'

const STATUS_TONE = {
  SCHEDULED: 'scheduled',
  COMPLETED: 'completed',
}

/** 게시판 목록만 (항목 클릭 시 상세 페이지로 이동) */
export function AssignmentVerifyList({ listTitle, items, emptyListMessage, getItemHref, activeId }) {
  return (
    <nav className="ams-assignment-board ams-assignment-board--list-only" aria-label={listTitle}>
      <div className="ams-assignment-board__list ams-card ams-card--elevated">
        <div className="ams-assignment-board__list-head">
          <h4 className="ams-assignment-board__list-title">{listTitle}</h4>
          <span className="ams-assignment-board__list-count">{items.length}</span>
        </div>

        {items.length === 0 ? (
          <p className="ams-assignment-board__list-empty">{emptyListMessage}</p>
        ) : (
          <ul className="ams-assignment-board__threads">
            {items.map((item) => {
              const active = activeId === item.id
              return (
                <li key={item.id}>
                  <Link
                    to={getItemHref(item.id)}
                    className={`ams-assignment-board__thread${active ? ' ams-assignment-board__thread--active' : ''}`}
                    aria-current={active ? 'page' : undefined}
                  >
                    <span
                      className={`ams-assignment-board__status ams-assignment-board__status--${STATUS_TONE[item.status] || 'scheduled'}`}
                    >
                      {item.statusLabel}
                    </span>
                    <span className="ams-assignment-board__thread-title">{item.title}</span>
                    {item.subtitle ? (
                      <span className="ams-assignment-board__thread-sub">{item.subtitle}</span>
                    ) : null}
                    {item.chips?.length > 0 && (
                      <span className="ams-assignment-board__thread-chips">
                        {item.chips.map((chip) => (
                          <span key={chip} className="ams-assignment-board__chip">
                            {chip}
                          </span>
                        ))}
                      </span>
                    )}
                  </Link>
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </nav>
  )
}

/** 상세 페이지 본문 */
export function AssignmentDetailPanel({ title, meta, toolbar, students, renderStudentCard }) {
  return (
    <main className="ams-assignment-detail ams-card ams-card--elevated">
      <header className="ams-assignment-board__detail-head">
        <div className="ams-assignment-board__detail-head-text">
          {title ? <h1 className="ams-assignment-detail__title">{title}</h1> : null}
          {meta ? <div className="ams-assignment-board__detail-meta">{meta}</div> : null}
        </div>
        {toolbar ? <div className="ams-assignment-board__detail-toolbar">{toolbar}</div> : null}
      </header>

      {students.length === 0 ? (
        <p className="ams-assignment-board__detail-empty">대상 학생이 없습니다.</p>
      ) : (
        <ul className="ams-assignment-board__students">
          {students.map((student) => (
            <li key={student.key}>{renderStudentCard(student)}</li>
          ))}
        </ul>
      )}
    </main>
  )
}

export function AssignmentStudentCard({ name, stats, statusLabel, statusTone, action }) {
  return (
    <article className="ams-assignment-board__student">
      <div className="ams-assignment-board__student-main">
        <strong className="ams-assignment-board__student-name">{name}</strong>
        {stats?.length > 0 && (
          <div className="ams-assignment-board__student-stats">
            {stats.map((stat) => (
              <span key={stat} className="ams-assignment-board__stat">
                {stat}
              </span>
            ))}
          </div>
        )}
        {statusLabel ? (
          <span
            className={`ams-assignment-board__student-status ams-assignment-board__student-status--${statusTone || 'pending'}`}
          >
            {statusLabel}
          </span>
        ) : null}
      </div>
      {action ? <div className="ams-assignment-board__student-action">{action}</div> : null}
    </article>
  )
}
