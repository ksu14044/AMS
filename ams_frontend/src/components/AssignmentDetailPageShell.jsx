import { Link } from 'react-router-dom'
import { subjectLabel } from '../auth/subjectLabels'

export default function AssignmentDetailPageShell({
  classId,
  className,
  classSubject,
  classRoom,
  listTab,
  listLabel,
  title,
  error,
  children,
}) {
  const listPath = `/classes/${classId}?tab=${listTab}`

  return (
    <div className="ams-class-detail ams-assignment-detail-page">
      <header className="ams-assignment-detail-page__header">
        <Link to={listPath} className="ams-assignment-detail-page__back">
          ← {listLabel} 목록
        </Link>
        <div className="ams-assignment-detail-page__header-body">
          <p className="ams-assignment-detail-page__class">
            {className}
            {classSubject ? ` · ${subjectLabel(classSubject)}` : ''}
            {classRoom ? ` · ${classRoom}` : ''}
          </p>
          {title ? <h2 className="ams-assignment-detail-page__heading">{title}</h2> : null}
        </div>
      </header>

      {error ? <p className="ams-class-detail__error">{error}</p> : null}

      {children}
    </div>
  )
}
