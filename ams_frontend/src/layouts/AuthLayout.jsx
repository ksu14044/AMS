import { Link, Outlet } from 'react-router-dom'
import '../styles/auth.css'

export default function AuthLayout() {
  return (
    <div className="ams-auth">
      <header className="ams-auth__header">
        <Link to="/login" className="ams-auth__logo">
          AMS
        </Link>
      </header>
      <main className="ams-auth__main">
        <Outlet />
      </main>
    </div>
  )
}
