import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import {
  fetchClassSchedule,
  fetchClasses,
  fetchHomeworks,
  fetchTests,
} from '../../api/classesApi'
import AssistantClinicWeekCard from '../../components/AssistantClinicWeekCard'
import StudentRosterHomeCard from '../../components/StudentRosterHomeCard'
import { fetchUnreadNotificationCount } from '../../api/notificationsApi'
import AcademyNoticesSection from '../../components/AcademyNoticesSection'
import AdminSignupInvitesSection from '../admin/AdminSignupInvitesSection'
import DashboardCard from '../../components/DashboardCard'
import { subjectLabel } from '../../auth/subjectLabels'
import { dayLabel } from '../../auth/dayLabels'
import '../../styles/class-list.css'
import '../../styles/class-detail.css'
import '../../styles/admin.css'

const DAY_ORDER = { MON: 1, TUE: 2, WED: 3, THU: 4, FRI: 5, SAT: 6, SUN: 7 }

function emptyStateForRole(role) {
  if (role?.startsWith('ASSISTANT_')) {
    return {
      title: '담당 반이 없습니다',
      desc: '담임 선생님이 반 상세의 「조교」 탭에서 담당 반을 지정하면 여기에 표시됩니다.',
    }
  }
  if (role === 'STAFF_OFFICE') {
    return {
      title: '담당 반이 없습니다',
      desc: '반 운영은 담임·조교 전용입니다. 학원 공지는 위에서 등록·확인할 수 있습니다.',
    }
  }
  return {
    title: '담임 반이 없습니다',
    desc: '관리자가 반을 만들 때 담임으로 지정되면 이 화면에 반 목록이 표시됩니다.',
  }
}

function listHeadingForRole(role) {
  if (role?.startsWith('ASSISTANT_')) return '담당 반'
  if (role === 'STAFF_OFFICE') return '반 목록 (참고)'
  return '담임 반'
}

function isDueWithinDays(iso, days = 7) {
  const due = new Date(iso).getTime()
  const now = Date.now()
  return due >= now && due <= now + days * 24 * 60 * 60 * 1000
}

function sortEventKey(dayOfWeek, time) {
  const day = DAY_ORDER[dayOfWeek] ?? 9
  return `${day}-${time ?? ''}`
}

function formatShortDue(iso) {
  return new Date(iso).toLocaleString('ko-KR', {
    month: 'numeric',
    day: 'numeric',
    weekday: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function loadTeacherAgenda(classes) {
  const events = []
  await Promise.all(
    classes.map(async (c) => {
      const [slots, homeworks, tests] = await Promise.all([
        fetchClassSchedule(c.classId),
        fetchHomeworks(c.classId),
        fetchTests(c.classId),
      ])
      for (const slot of slots) {
        const time = slot.startTime?.slice(0, 5) ?? ''
        events.push({
          sortKey: sortEventKey(slot.dayOfWeek, time),
          label: `${dayLabel(slot.dayOfWeek)} ${time} ${c.name} 수업`,
        })
      }
      for (const hw of homeworks) {
        if (hw.status === 'SCHEDULED' && isDueWithinDays(hw.dueAt)) {
          const due = new Date(hw.dueAt)
          events.push({
            sortKey: `${due.getDay()}-${due.toISOString()}`,
            label: `${formatShortDue(hw.dueAt)} 숙제 마감 — ${hw.title} (${c.name})`,
          })
        }
      }
      for (const test of tests) {
        if (test.status === 'SCHEDULED' && isDueWithinDays(test.testAt)) {
          events.push({
            sortKey: `${new Date(test.testAt).getDay()}-${test.testAt}`,
            label: `${formatShortDue(test.testAt)} 테스트 — ${test.title} (${c.name})`,
          })
        }
      }
    }),
  )
  return events.sort((a, b) => a.sortKey.localeCompare(b.sortKey)).slice(0, 8)
}

export default function StaffHomePage() {
  const { user } = useAuth()
  const [classes, setClasses] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [agenda, setAgenda] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const isOffice = user?.role === 'STAFF_OFFICE'
  const isAssistant = user?.role?.startsWith('ASSISTANT_')
  const isTeacher = user?.role?.startsWith('TEACHER_')
  const emptyState = useMemo(() => emptyStateForRole(user?.role), [user?.role])
  const listHeading = useMemo(() => listHeadingForRole(user?.role), [user?.role])

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      try {
        setUnreadCount(await fetchUnreadNotificationCount())
      } catch {
        setUnreadCount(0)
      }
      if (isOffice) {
        setClasses([])
        setAgenda([])
        return
      }
      const list = await fetchClasses()
      setClasses(list)
      if (isTeacher && list.length > 0) {
        setAgenda(await loadTeacherAgenda(list))
      } else {
        setAgenda([])
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [isAssistant, isTeacher, isOffice])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div className="ams-class-list-page">
      {user?.name && (
        <p className="ams-class-list-page__greeting">
          안녕하세요, {user.name}님
        </p>
      )}

      {unreadCount > 0 && (
        <DashboardCard title="알림" actionLabel={`미확인 ${unreadCount}건 →`} actionTo="/notifications">
          <p className="ams-class-home__text">확인하지 않은 알림이 있습니다.</p>
        </DashboardCard>
      )}

      <AcademyNoticesSection canManage={isOffice} compact={!isOffice} />

      {isOffice && <AdminSignupInvitesSection />}

      {(isOffice || isTeacher) && <StudentRosterHomeCard />}

      {isTeacher && agenda.length > 0 && (
        <DashboardCard title="이번 주 일정">
          <ul className="ams-class-home__list">
            {agenda.map((item) => (
              <li key={item.label}>{item.label}</li>
            ))}
          </ul>
        </DashboardCard>
      )}

      {isAssistant && <AssistantClinicWeekCard userId={user?.userId} />}

      {isOffice && !loading && (
        <div className="ams-class-list-page__card ams-class-list-page__card--accent">
          <h2 className="ams-class-list-page__title">학원 운영 안내</h2>
          <p className="ams-class-list-page__desc">
            반 운영(수업·숙제·클리닉 등)은 담임·조교 전용입니다. 행정 업무는 학원 공지 등록·확인
            중심으로 이용해 주세요.
          </p>
        </div>
      )}

      {!isOffice &&
        (loading ? (
          <p className="ams-class-list-page__empty">불러오는 중…</p>
        ) : error ? (
          <p className="ams-class-list-page__error">{error}</p>
        ) : classes.length === 0 ? (
          <div className="ams-class-list-page__card ams-class-list-page__card--accent">
            <h2 className="ams-class-list-page__title">{emptyState.title}</h2>
            <p className="ams-class-list-page__desc">{emptyState.desc}</p>
          </div>
        ) : (
          <>
            <p className="ams-class-list-page__lead">{listHeading}</p>
            <div className="ams-class-list">
              {classes.map((c) => (
                <Link key={c.classId} to={`/classes/${c.classId}`} className="ams-class-list__item">
                  <div className="ams-class-list__row">
                    <strong className="ams-class-list__name">{c.name}</strong>
                    <span className="ams-class-list__badge">{subjectLabel(c.subject)}</span>
                  </div>
                  {c.classroom && (
                    <span className="ams-class-list__meta">{c.classroom}</span>
                  )}
                </Link>
              ))}
            </div>
          </>
        ))}

      {isOffice && loading && (
        <p className="ams-class-list-page__empty">불러오는 중…</p>
      )}
    </div>
  )
}
