import { Link } from 'react-router-dom'

export default function DashboardCard({ title, actionLabel, actionTo, onAction, children }) {
  return (
    <section className="ams-class-home__card">
      <header className="ams-class-home__card-head">
        <h3 className="ams-class-home__card-title">{title}</h3>
        {actionLabel && actionTo && (
          <Link to={actionTo} className="ams-class-home__link">
            {actionLabel}
          </Link>
        )}
        {actionLabel && onAction && !actionTo && (
          <button type="button" className="ams-class-home__link" onClick={onAction}>
            {actionLabel}
          </button>
        )}
      </header>
      {children}
    </section>
  )
}
