import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import StudentRosterSection from '../../components/StudentRosterSection'
import { studentRosterDescriptionForRole } from '../../utils/studentRosterPaths'
import '../../styles/student-roster-page.css'
import '../../styles/admin.css'

export default function StudentRosterPage() {
  const { user, homePath } = useAuth()

  return (
    <div className="ams-student-roster-page">
      <Link to={homePath} className="ams-student-roster-page__back">
        ← 홈으로
      </Link>
      <StudentRosterSection
        variant="table"
        description={studentRosterDescriptionForRole(user?.role)}
      />
    </div>
  )
}
