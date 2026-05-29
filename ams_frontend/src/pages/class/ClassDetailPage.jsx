import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { fetchClassDetail, fetchClassNotices } from '../../api/classesApi'
import { useAuth } from '../../auth/AuthContext'
import { homePathForRole } from '../../auth/roleLabels'
import { subjectLabel } from '../../auth/subjectLabels'
import ClassHomeSection from './ClassHomeSection'
import ClassNoticesSection from './ClassNoticesSection'
import ClassScheduleSection from './ClassScheduleSection'
import ClassTextbookSection from './ClassTextbookSection'
import ClassHomeworkSection from './ClassHomeworkSection'
import ClassTestSection from './ClassTestSection'
import ClassVideoSection from './ClassVideoSection'
import ClassClinicSection from './ClassClinicSection'
import ClassAssistantsSection from './ClassAssistantsSection'
import ClassStudyRecordSection from './ClassStudyRecordSection'
import ClassReportsSection from './ClassReportsSection'
import '../../styles/class-detail.css'

const BASE_TABS = [
  { id: 'notices', label: '공지' },
  { id: 'schedule', label: '수업정보' },
  { id: 'textbook', label: '교재' },
  { id: 'video', label: '영상' },
  { id: 'homework', label: '숙제' },
  { id: 'test', label: '테스트' },
  { id: 'clinic', label: '클리닉' },
  { id: 'study', label: '공부기록' },
]

const ASSISTANT_ROLES = ['ASSISTANT_KO', 'ASSISTANT_EN', 'ASSISTANT_MATH']

export default function ClassDetailPage() {
  const { classId } = useParams()
  const [searchParams] = useSearchParams()
  const tabFromUrl = searchParams.get('tab')
  const clinicDayFromUrl = searchParams.get('clinicDay')
  const initialClinicDay = ['MON', 'TUE', 'WED', 'THU', 'FRI'].includes(clinicDayFromUrl)
    ? clinicDayFromUrl
    : undefined
  const { user } = useAuth()
  const homePath = homePathForRole(user?.role)
  const isAssistant = ASSISTANT_ROLES.includes(user?.role)
  const isStudent = user?.role === 'STUDENT'

  const [activeTab, setActiveTab] = useState(() => (user?.role === 'STUDENT' ? 'home' : 'notices'))
  const [detail, setDetail] = useState(null)
  const [notices, setNotices] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const tabs = useMemo(
    () => [
      ...(isStudent ? [{ id: 'home', label: '홈' }] : []),
      ...BASE_TABS,
      ...(isAssistant ? [] : [{ id: 'reports', label: '보고서' }]),
      ...(detail?.canManageContent ? [{ id: 'assistants', label: '조교' }] : []),
    ],
    [isStudent, isAssistant, detail?.canManageContent],
  )

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [d, n] = await Promise.all([
        fetchClassDetail(classId),
        fetchClassNotices(classId),
      ])
      setDetail(d)
      setNotices(n)
    } catch (err) {
      setError(err.message)
      setDetail(null)
    } finally {
      setLoading(false)
    }
  }, [classId])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (!tabFromUrl) return
    if (tabs.some((t) => t.id === tabFromUrl && !t.disabled)) {
      setActiveTab(tabFromUrl)
    }
  }, [tabFromUrl, tabs])

  if (loading) {
    return <p className="ams-class-detail__empty">불러오는 중…</p>
  }

  if (error && !detail) {
    return (
      <div className="ams-class-detail">
        <p className="ams-class-detail__error">{error}</p>
        <Link to={homePath} className="ams-class-detail__back">
          목록으로
        </Link>
      </div>
    )
  }

  return (
    <div className="ams-class-detail">
      <header className="ams-class-detail__header">
        <Link to={homePath} className="ams-icon-btn ams-class-detail__back" aria-label="목록으로">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden
          >
            <path d="M15 5 8 12l7 7" />
          </svg>
        </Link>
        <div className="ams-class-detail__header-body">
          <h2 className="ams-class-detail__title">{detail.name}</h2>
          <p className="ams-class-detail__subtitle">
            {subjectLabel(detail.subject)}
            {detail.classroom ? ` · ${detail.classroom}` : ''}
          </p>
        </div>
        <button
          type="button"
          className="ams-icon-btn ams-class-detail__more"
          aria-label="반 메뉴"
          aria-haspopup="menu"
          aria-disabled="true"
          title="추가 메뉴 (준비 중)"
          disabled
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden
          >
            <circle cx="12" cy="5.5" r="1.2" />
            <circle cx="12" cy="12" r="1.2" />
            <circle cx="12" cy="18.5" r="1.2" />
          </svg>
        </button>
      </header>

      <nav className="ams-class-detail__tabs" aria-label="반 섹션">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            type="button"
            className={
              tab.disabled
                ? 'ams-class-detail__tab ams-class-detail__tab--disabled'
                : activeTab === tab.id
                  ? 'ams-class-detail__tab ams-class-detail__tab--active'
                  : 'ams-class-detail__tab'
            }
            disabled={tab.disabled}
            onClick={() => !tab.disabled && setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </nav>

      {error && <p className="ams-class-detail__error">{error}</p>}

      {activeTab === 'home' && isStudent && (
        <ClassHomeSection
          classId={classId}
          notices={notices}
          onGoTab={setActiveTab}
          onError={setError}
        />
      )}

      {activeTab === 'notices' && (
        <ClassNoticesSection
          classId={classId}
          canManage={detail.canManageContent}
          notices={notices}
          onNoticesChange={setNotices}
          onError={setError}
        />
      )}

      {activeTab === 'schedule' && (
        <ClassScheduleSection
          classId={classId}
          canManage={detail.canManageContent}
          onError={setError}
        />
      )}

      {activeTab === 'textbook' && (
        <ClassTextbookSection
          classId={classId}
          canManage={detail.canManageContent}
          onError={setError}
        />
      )}

      {activeTab === 'video' && (
        <ClassVideoSection
          classId={classId}
          canManage={detail.canManageContent}
          isStudent={isStudent}
          onError={setError}
        />
      )}

      {activeTab === 'homework' && (
        <ClassHomeworkSection
          classId={classId}
          canManage={detail.canManageContent}
          onError={setError}
        />
      )}

      {activeTab === 'test' && (
        <ClassTestSection
          classId={classId}
          canManage={detail.canManageContent}
          onError={setError}
        />
      )}

      {activeTab === 'clinic' && (
        <ClassClinicSection
          classId={classId}
          canManage={detail.canManageContent}
          isStudent={isStudent}
          canViewResults={user?.role !== 'STUDENT'}
          initialClinicDay={initialClinicDay}
          onError={setError}
        />
      )}

      {activeTab === 'study' && (
        <ClassStudyRecordSection classId={classId} isStudent={isStudent} onError={setError} />
      )}

      {activeTab === 'reports' && !isAssistant && (
        <ClassReportsSection
          classId={classId}
          canManage={detail.canManageContent}
          isStudent={isStudent}
          onError={setError}
        />
      )}

      {activeTab === 'assistants' && detail.canManageContent && (
        <ClassAssistantsSection classId={classId} onError={setError} />
      )}
    </div>
  )
}
