import { useCallback, useEffect, useState } from 'react'
import { fetchStudentRoster } from '../api/studentsApi'
import { subjectLabel } from '../auth/subjectLabels'
import { formatPhoneNumber } from '../utils/phoneFormat'

function formatJoinedAt(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}

function statusLabel(status) {
  if (status === 'PENDING') return '승인 대기'
  if (status === 'ACTIVE') return '활성'
  if (status === 'SUSPENDED') return '정지'
  return status ?? '—'
}

function formatClasses(classes) {
  if (!classes?.length) return '배정 대기'
  return classes.map((c) => `${c.name} (${subjectLabel(c.subject)})`).join(', ')
}

function StatusPill({ status }) {
  return (
    <span
      className={
        status === 'PENDING' ? 'ams-pill ams-pill--warning' : 'ams-pill ams-pill--muted'
      }
    >
      {statusLabel(status)}
    </span>
  )
}

export default function StudentRosterSection({
  title = '학생부',
  description,
  variant = 'table',
}) {
  const [rows, setRows] = useState([])
  const [search, setSearch] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setRows(await fetchStudentRoster(appliedSearch))
    } catch (err) {
      setError(err.message)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [appliedSearch])

  useEffect(() => {
    load()
  }, [load])

  function handleSearchSubmit(e) {
    e.preventDefault()
    setAppliedSearch(search.trim())
  }

  const isTable = variant === 'table'

  return (
    <section
      className={
        isTable
          ? 'ams-student-roster ams-student-roster--page'
          : 'ams-admin__section ams-student-roster'
      }
    >
      {!isTable && <h2 className="ams-admin__heading">{title}</h2>}
      {description && <p className="ams-admin__desc">{description}</p>}

      <form className="ams-student-roster__search" onSubmit={handleSearchSubmit}>
        <label className="ams-student-roster__search-label">
          <input
            aria-label="학생 검색"
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="이름, 이메일, 전화번호, 반 이름"
            className="ams-student-roster__search-input"
          />
        </label>
        <button type="submit" className="ams-btn ams-btn--primary ams-btn--sm">
          검색
        </button>
        {appliedSearch && (
          <button
            type="button"
            className="ams-btn ams-btn--ghost ams-btn--sm"
            onClick={() => {
              setSearch('')
              setAppliedSearch('')
            }}
          >
            초기화
          </button>
        )}
      </form>

      {error && <p className="ams-admin__error">{error}</p>}

      {loading ? (
        <p className="ams-admin__empty">불러오는 중…</p>
      ) : rows.length === 0 ? (
        <p className="ams-admin__empty">
          {appliedSearch ? '검색 결과가 없습니다.' : '등록된 학생이 없습니다.'}
        </p>
      ) : isTable ? (
        <div className="ams-student-roster__table-wrap">
          <table className="ams-student-roster__table">
            <thead>
              <tr>
                <th scope="col">이름</th>
                <th scope="col">이메일</th>
                <th scope="col">수강 반</th>
                <th scope="col">전화번호</th>
                <th scope="col">가입일</th>
                <th scope="col">상태</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.userId}>
                  <td>
                    <strong>{row.name}</strong>
                  </td>
                  <td className="ams-student-roster__email">{row.email}</td>
                  <td className="ams-student-roster__classes">{formatClasses(row.classes)}</td>
                  <td className="ams-student-roster__phone">
                    {row.phoneNumber ? formatPhoneNumber(row.phoneNumber) : '—'}
                  </td>
                  <td className="ams-student-roster__date">{formatJoinedAt(row.createdAt)}</td>
                  <td>
                    <StatusPill status={row.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <p className="ams-student-roster__count">총 {rows.length}명</p>
        </div>
      ) : (
        <div className="ams-student-roster__list-wrap">
          <ul className="ams-student-roster__list">
            {rows.map((row) => (
              <li key={row.userId} className="ams-student-roster__card">
                <div className="ams-student-roster__line">
                  <span className="ams-student-roster__label">이름</span>
                  <span className="ams-student-roster__value">
                    <strong>{row.name}</strong>
                  </span>
                </div>
                <div className="ams-student-roster__line">
                  <span className="ams-student-roster__label">이메일</span>
                  <span className="ams-student-roster__value">{row.email}</span>
                </div>
                <div className="ams-student-roster__line">
                  <span className="ams-student-roster__label">수강 반</span>
                  <span className="ams-student-roster__value">{formatClasses(row.classes)}</span>
                </div>
                <div className="ams-student-roster__line">
                  <span className="ams-student-roster__label">전화번호</span>
                  <span className="ams-student-roster__value">
                    {row.phoneNumber ? formatPhoneNumber(row.phoneNumber) : '—'}
                  </span>
                </div>
                <div className="ams-student-roster__line">
                  <span className="ams-student-roster__label">가입일</span>
                  <span className="ams-student-roster__value">{formatJoinedAt(row.createdAt)}</span>
                </div>
                <div className="ams-student-roster__line">
                  <span className="ams-student-roster__label">상태</span>
                  <span className="ams-student-roster__value">
                    <StatusPill status={row.status} />
                  </span>
                </div>
              </li>
            ))}
          </ul>
          <p className="ams-student-roster__count">총 {rows.length}명</p>
        </div>
      )}
    </section>
  )
}
