import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from '../auth/AuthContext'
import { GuestRoute, ProtectedRoute } from '../components/ProtectedRoute'
import AppLayout from '../layouts/AppLayout'
import AuthLayout from '../layouts/AuthLayout'
import LoginAcademySelectPage from '../pages/auth/LoginAcademySelectPage'
import LoginPage from '../pages/auth/LoginPage'
import PendingApprovalPage from '../pages/auth/PendingApprovalPage'
import SignupAcademyPage from '../pages/auth/SignupAcademyPage'
import SignupStaffPage from '../pages/auth/SignupStaffPage'
import SignupStudentPage from '../pages/auth/SignupStudentPage'
import NotificationsPage from '../pages/notifications/NotificationsPage'
import AdminHomePage from '../pages/admin/AdminHomePage'
import ClassDetailPage from '../pages/class/ClassDetailPage'
import StudentHomePage from '../pages/student/StudentHomePage'
import StudentClinicPage from '../pages/student/StudentClinicPage'
import StudentRecordsPage from '../pages/student/StudentRecordsPage'
import StudentMyPage from '../pages/student/StudentMyPage'
import StaffHomePage from '../pages/staff/StaffHomePage'
import StudentRosterPage from '../pages/students/StudentRosterPage'

function RootRedirect() {
  const { isAuthenticated, user, homePath } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (user?.status === 'PENDING') return <Navigate to="/pending" replace />
  return <Navigate to={homePath} replace />
}

export default function AppRouter() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<RootRedirect />} />

          <Route element={<GuestRoute />}>
            <Route element={<AuthLayout />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/login/select" element={<LoginAcademySelectPage />} />
              <Route path="/signup/academy" element={<SignupAcademyPage />} />
              <Route path="/signup/staff" element={<SignupStaffPage />} />
              <Route path="/signup/student" element={<SignupStudentPage />} />
            </Route>
          </Route>

          <Route element={<AuthLayout />}>
            <Route path="/pending" element={<PendingApprovalPage />} />
          </Route>

          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout title="알림" />}>
              <Route path="/notifications" element={<NotificationsPage />} />
            </Route>
            <Route element={<AppLayout title="반 상세" />}>
              <Route path="/classes/:classId" element={<ClassDetailPage />} />
            </Route>
          </Route>

          <Route element={<ProtectedRoute allowedRoles={['ACADEMY_ADMIN']} />}>
            <Route element={<AppLayout title="관리자 홈" />}>
              <Route path="/admin" element={<AdminHomePage />} />
            </Route>
            <Route element={<AppLayout title="학생부" />}>
              <Route path="/admin/students" element={<StudentRosterPage />} />
            </Route>
          </Route>

          <Route
            element={
              <ProtectedRoute
                allowedRoles={['TEACHER_KO', 'TEACHER_EN', 'TEACHER_MATH', 'STAFF_OFFICE']}
              />
            }
          >
            <Route element={<AppLayout title="선생님 홈" />}>
              <Route path="/teacher" element={<StaffHomePage />} />
            </Route>
            <Route element={<AppLayout title="학생부" />}>
              <Route path="/teacher/students" element={<StudentRosterPage />} />
            </Route>
          </Route>

          <Route
            element={
              <ProtectedRoute allowedRoles={['ASSISTANT_KO', 'ASSISTANT_EN', 'ASSISTANT_MATH']} />
            }
          >
            <Route element={<AppLayout title="조교 홈" />}>
              <Route path="/assistant" element={<StaffHomePage />} />
            </Route>
          </Route>

          <Route element={<ProtectedRoute allowedRoles={['STUDENT']} />}>
            <Route element={<AppLayout title="학생 홈" />}>
              <Route path="/student" element={<StudentHomePage />} />
              <Route path="/student/clinic" element={<StudentClinicPage />} />
              <Route path="/student/records" element={<StudentRecordsPage />} />
              <Route path="/student/my" element={<StudentMyPage />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
