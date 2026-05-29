import { useAuth } from '../auth/AuthContext'
import { studentRosterDescriptionForRole, studentRosterPathForRole } from '../utils/studentRosterPaths'
import DashboardCard from './DashboardCard'

export default function StudentRosterHomeCard() {
  const { user } = useAuth()
  const path = studentRosterPathForRole(user?.role)
  const description = studentRosterDescriptionForRole(user?.role)

  if (!path) return null

  return (
    <DashboardCard title="학생부" actionLabel="전체 보기 →" actionTo={path}>
      <p className="ams-class-home__text">{description}</p>
    </DashboardCard>
  )
}
