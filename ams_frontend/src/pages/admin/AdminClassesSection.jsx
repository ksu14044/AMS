import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  createClass,
  enrollStudent,
  fetchClasses,
  fetchEnrollments,
  fetchStudents,
  fetchTeachers,
  unenrollStudent,
  updateClass,
} from '../../api/adminApi'
import { SUBJECT_OPTIONS, subjectLabel } from '../../auth/subjectLabels'

const EMPTY_CLASS_FORM = {
  subject: 'KO',
  name: '',
  homeroomTeacherId: '',
  classroom: '',
}

export default function AdminClassesSection() {
  const [classes, setClasses] = useState([])
  const [teachers, setTeachers] = useState([])
  const [students, setStudents] = useState([])
  const [enrollments, setEnrollments] = useState([])
  const [selectedClassId, setSelectedClassId] = useState('')
  const [classForm, setClassForm] = useState(EMPTY_CLASS_FORM)
  const [enrollStudentId, setEnrollStudentId] = useState('')
  const [editForm, setEditForm] = useState(null)
  const [loading, setLoading] = useState(true)
  const [enrollmentsLoading, setEnrollmentsLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const loadBase = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [classList, teacherList, studentList] = await Promise.all([
        fetchClasses(),
        fetchTeachers(),
        fetchStudents(),
      ])
      setClasses(classList)
      setTeachers(teacherList)
      setStudents(studentList)
      setSelectedClassId((prev) => {
        if (prev && classList.some((c) => String(c.classId) === prev)) return prev
        return classList.length > 0 ? String(classList[0].classId) : ''
      })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  const loadEnrollments = useCallback(async (classId) => {
    if (!classId) {
      setEnrollments([])
      return
    }
    setEnrollmentsLoading(true)
    try {
      const list = await fetchEnrollments(classId)
      setEnrollments(list)
    } catch (err) {
      setError(err.message)
    } finally {
      setEnrollmentsLoading(false)
    }
  }, [])

  useEffect(() => {
    loadBase()
  }, [loadBase])

  useEffect(() => {
    if (selectedClassId) {
      loadEnrollments(selectedClassId)
    }
  }, [selectedClassId, loadEnrollments])

  const selectedClass = useMemo(
    () => classes.find((c) => String(c.classId) === selectedClassId),
    [classes, selectedClassId],
  )

  useEffect(() => {
    if (!selectedClass) {
      setEditForm(null)
      return
    }
    setEditForm({
      subject: selectedClass.subject,
      name: selectedClass.name,
      homeroomTeacherId: String(selectedClass.homeroomTeacherId),
      classroom: selectedClass.classroom ?? '',
    })
  }, [selectedClass])

  const teachersForSubject = useMemo(
    () => teachers.filter((t) => t.subject === classForm.subject),
    [teachers, classForm.subject],
  )

  const studentNameById = useMemo(() => {
    const map = {}
    for (const s of students) {
      map[s.userId] = s.name
    }
    return map
  }, [students])

  const enrolledStudentIds = useMemo(
    () => new Set(enrollments.map((e) => e.studentId)),
    [enrollments],
  )

  const studentsAvailableToEnroll = useMemo(
    () => students.filter((s) => !enrolledStudentIds.has(s.userId)),
    [students, enrolledStudentIds],
  )

  const teacherNameById = useMemo(() => {
    const map = {}
    for (const t of teachers) {
      map[t.userId] = `${t.name} (${subjectLabel(t.subject)})`
    }
    return map
  }, [teachers])

  async function handleCreateClass(e) {
    e.preventDefault()
    if (!classForm.name.trim() || !classForm.homeroomTeacherId) {
      setError('반 이름과 담임 선생님을 입력해 주세요.')
      return
    }
    setSubmitting(true)
    setError('')
    try {
      await createClass({
        subject: classForm.subject,
        name: classForm.name.trim(),
        homeroomTeacherId: Number(classForm.homeroomTeacherId),
        classroom: classForm.classroom.trim() || null,
      })
      setClassForm({ ...EMPTY_CLASS_FORM, subject: classForm.subject })
      await loadBase()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleEnroll(e) {
    e.preventDefault()
    if (!selectedClassId || !enrollStudentId) return
    setSubmitting(true)
    setError('')
    try {
      await enrollStudent(selectedClassId, Number(enrollStudentId))
      setEnrollStudentId('')
      await loadEnrollments(selectedClassId)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleUpdateClass(e) {
    e.preventDefault()
    if (!selectedClassId || !editForm) return
    setSubmitting(true)
    setError('')
    try {
      await updateClass(selectedClassId, {
        subject: editForm.subject,
        name: editForm.name.trim(),
        homeroomTeacherId: Number(editForm.homeroomTeacherId),
        classroom: editForm.classroom.trim() || null,
      })
      await loadBase()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleUnenroll(enrollmentId) {
    setSubmitting(true)
    setError('')
    try {
      await unenrollStudent(enrollmentId)
      await loadEnrollments(selectedClassId)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      {error && <p className="ams-admin__error ams-admin__error--block">{error}</p>}

      <section className="ams-admin__section">
        <h2 className="ams-admin__heading">반 생성</h2>
        <p className="ams-admin__desc">과목·반 이름·담임을 지정해 새 반을 만듭니다.</p>

        {loading ? (
          <p className="ams-admin__empty">불러오는 중…</p>
        ) : (
          <form className="ams-admin-form" onSubmit={handleCreateClass}>
            <div className="ams-admin-form__row">
              <label>
                과목
                <select
                  value={classForm.subject}
                  onChange={(e) =>
                    setClassForm({
                      ...classForm,
                      subject: e.target.value,
                      homeroomTeacherId: '',
                    })
                  }
                >
                  {SUBJECT_OPTIONS.map((s) => (
                    <option key={s.value} value={s.value}>
                      {s.label}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                반 이름
                <input
                  type="text"
                  value={classForm.name}
                  onChange={(e) => setClassForm({ ...classForm, name: e.target.value })}
                  placeholder="예: 국어 남고1반"
                  maxLength={100}
                />
              </label>
            </div>
            <div className="ams-admin-form__row">
              <label>
                담임 선생님
                <select
                  value={classForm.homeroomTeacherId}
                  onChange={(e) =>
                    setClassForm({ ...classForm, homeroomTeacherId: e.target.value })
                  }
                >
                  <option value="">선택</option>
                  {teachersForSubject.map((t) => (
                    <option key={t.userId} value={t.userId}>
                      {t.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                강의실 (선택)
                <input
                  type="text"
                  value={classForm.classroom}
                  onChange={(e) => setClassForm({ ...classForm, classroom: e.target.value })}
                  placeholder="예: 301"
                  maxLength={50}
                />
              </label>
            </div>
            {teachersForSubject.length === 0 && (
              <p className="ams-admin__hint">해당 과목의 활성 담임 선생님이 없습니다. 교직원 승인 후 다시 시도하세요.</p>
            )}
            <button
              type="submit"
              className="ams-btn ams-btn--primary"
              disabled={submitting || teachersForSubject.length === 0}
            >
              {submitting ? '처리 중…' : '반 만들기'}
            </button>
          </form>
        )}
      </section>

      <section className="ams-admin__section">
        <h2 className="ams-admin__heading">반 목록</h2>
        {classes.length === 0 ? (
          <p className="ams-admin__empty">등록된 반이 없습니다.</p>
        ) : (
          <div className="ams-class-list">
            {classes.map((c) => (
              <Link key={c.classId} to={`/classes/${c.classId}`} className="ams-class-list__item">
                <div className="ams-class-list__row">
                  <strong className="ams-class-list__name">{c.name}</strong>
                  <span className="ams-class-list__badge">{subjectLabel(c.subject)}</span>
                </div>
                <span className="ams-class-list__meta">
                  담임: {teacherNameById[c.homeroomTeacherId] ?? `#${c.homeroomTeacherId}`}
                  {c.classroom ? ` · ${c.classroom}` : ''}
                </span>
              </Link>
            ))}
          </div>
        )}
      </section>

      {selectedClass && editForm && (
        <section className="ams-admin__section">
          <h2 className="ams-admin__heading">반 수정</h2>
          <p className="ams-admin__desc">선택한 반의 정보를 변경합니다 (수강 배정 아래 반 선택과 연동).</p>
          <form className="ams-admin-form" onSubmit={handleUpdateClass}>
            <div className="ams-admin-form__row">
              <label>
                과목
                <select
                  value={editForm.subject}
                  onChange={(e) =>
                    setEditForm({ ...editForm, subject: e.target.value, homeroomTeacherId: '' })
                  }
                >
                  {SUBJECT_OPTIONS.map((s) => (
                    <option key={s.value} value={s.value}>
                      {s.label}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                반 이름
                <input
                  type="text"
                  value={editForm.name}
                  onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                  maxLength={100}
                  required
                />
              </label>
            </div>
            <div className="ams-admin-form__row">
              <label>
                담임
                <select
                  value={editForm.homeroomTeacherId}
                  onChange={(e) =>
                    setEditForm({ ...editForm, homeroomTeacherId: e.target.value })
                  }
                >
                  <option value="">선택</option>
                  {teachers
                    .filter((t) => t.subject === editForm.subject)
                    .map((t) => (
                      <option key={t.userId} value={t.userId}>
                        {t.name}
                      </option>
                    ))}
                </select>
              </label>
              <label>
                강의실
                <input
                  type="text"
                  value={editForm.classroom}
                  onChange={(e) => setEditForm({ ...editForm, classroom: e.target.value })}
                  maxLength={50}
                />
              </label>
            </div>
            <button type="submit" className="ams-btn ams-btn--primary" disabled={submitting}>
              {submitting ? '저장 중…' : '변경 저장'}
            </button>
          </form>
        </section>
      )}

      <section className="ams-admin__section">
        <h2 className="ams-admin__heading">학생 수강 배정</h2>
        <p className="ams-admin__desc">반을 선택한 뒤 학생을 배정하거나 해제합니다.</p>

        {classes.length === 0 ? (
          <p className="ams-admin__empty">먼저 반을 생성해 주세요.</p>
        ) : (
          <>
            <label className="ams-admin-form__full">
              대상 반
              <select
                value={selectedClassId}
                onChange={(e) => setSelectedClassId(e.target.value)}
              >
                {classes.map((c) => (
                  <option key={c.classId} value={c.classId}>
                    [{subjectLabel(c.subject)}] {c.name}
                  </option>
                ))}
              </select>
            </label>

            <form className="ams-admin-form ams-admin-form--inline" onSubmit={handleEnroll}>
              <label>
                학생
                <select
                  value={enrollStudentId}
                  onChange={(e) => setEnrollStudentId(e.target.value)}
                >
                  <option value="">선택</option>
                  {studentsAvailableToEnroll.map((s) => (
                    <option key={s.userId} value={s.userId}>
                      {s.name} ({s.email})
                    </option>
                  ))}
                </select>
              </label>
              <button
                type="submit"
                className="ams-btn ams-btn--primary"
                disabled={submitting || !enrollStudentId}
              >
                배정
              </button>
            </form>

            {studentsAvailableToEnroll.length === 0 && students.length > 0 && (
              <p className="ams-admin__hint">배정 가능한 학생이 없습니다 (전원 배정됨 또는 학생 없음).</p>
            )}
            {students.length === 0 && (
              <p className="ams-admin__hint">활성 학생이 없습니다. 학생 가입·승인 후 배정할 수 있습니다.</p>
            )}

            <h3 className="ams-admin__subheading">수강생</h3>
            {enrollmentsLoading ? (
              <p className="ams-admin__empty">불러오는 중…</p>
            ) : enrollments.length === 0 ? (
              <p className="ams-admin__empty">배정된 학생이 없습니다.</p>
            ) : (
              <ul className="ams-enrollment-list">
                {enrollments.map((e) => (
                  <li key={e.enrollmentId} className="ams-enrollment-list__item">
                    <span>{studentNameById[e.studentId] ?? `학생 #${e.studentId}`}</span>
                    <button
                      type="button"
                      className="ams-btn ams-btn--ghost ams-btn--sm"
                      disabled={submitting}
                      onClick={() => handleUnenroll(e.enrollmentId)}
                    >
                      배정 해제
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
      </section>

    </>
  )
}
