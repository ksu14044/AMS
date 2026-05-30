import { useCallback, useEffect, useMemo, useState } from 'react'
import { fetchStudyRecordStudents } from '../api/studyRecordsApi'

/**
 * @param {object} props
 * @param {number} props.classId
 * @param {boolean} props.allByDefault 숙제·테스트·클리닉 true, 영상 false
 * @param {{ mode: 'all' | 'custom', studentIds: number[] }} props.value
 * @param {(value: { mode: 'all' | 'custom', studentIds: number[] }) => void} props.onChange
 * @param {boolean} [props.disabled]
 * @param {string} [props.className]
 * @param {string} [props.label]
 * @param {string} [props.hint]
 */
export default function StudentTargetPicker({
  classId,
  allByDefault,
  value,
  onChange,
  disabled = false,
  className = '',
  label = '대상 학생',
  hint,
}) {
  const [students, setStudents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadStudents = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setStudents(await fetchStudyRecordStudents(classId))
    } catch (err) {
      setError(err.message)
      setStudents([])
    } finally {
      setLoading(false)
    }
  }, [classId])

  useEffect(() => {
    loadStudents()
  }, [loadStudents])

  const selectedSet = useMemo(() => new Set(value.studentIds), [value.studentIds])

  function setMode(mode) {
    if (mode === 'all') {
      onChange({ mode: 'all', studentIds: [] })
      return
    }
    onChange({
      mode: 'custom',
      studentIds: value.mode === 'custom' ? value.studentIds : students.map((s) => s.studentId),
    })
  }

  function toggleStudent(studentId) {
    const next = new Set(selectedSet)
    if (next.has(studentId)) {
      next.delete(studentId)
    } else {
      next.add(studentId)
    }
    onChange({ mode: 'custom', studentIds: [...next] })
  }

  function selectAll() {
    onChange({ mode: 'custom', studentIds: students.map((s) => s.studentId) })
  }

  function clearAll() {
    onChange({ mode: 'custom', studentIds: [] })
  }

  const hintText = hint ?? (allByDefault
    ? '미지정 시 반 전원이 대상입니다. 일부 학생만 지정하려면 「학생 선택」을 사용하세요.'
    : '모든 수강생이 영상을 시청할 수 있습니다. 인증 사진을 제출할 학생만 선택하세요. (미선택 시 인증 없음)')

  return (
    <div className={`ams-target-picker${className ? ` ${className}` : ''}`}>
      <span className="ams-target-picker__label">{label}</span>
      <p className="ams-target-picker__hint">{hintText}</p>

      {allByDefault && (
        <div className="ams-target-picker__mode">
          <label className="ams-target-picker__mode-option">
            <input
              type="radio"
              name={`target-mode-${classId}`}
              checked={value.mode === 'all'}
              disabled={disabled}
              onChange={() => setMode('all')}
            />
            반 전원
          </label>
          <label className="ams-target-picker__mode-option">
            <input
              type="radio"
              name={`target-mode-${classId}`}
              checked={value.mode === 'custom'}
              disabled={disabled}
              onChange={() => setMode('custom')}
            />
            학생 선택
          </label>
        </div>
      )}

      {(!allByDefault || value.mode === 'custom') && (
        <div className="ams-target-picker__panel">
          {loading ? (
            <p className="ams-target-picker__status">학생 목록 불러오는 중…</p>
          ) : error ? (
            <p className="ams-target-picker__status ams-target-picker__status--error">{error}</p>
          ) : students.length === 0 ? (
            <p className="ams-target-picker__status">등록된 수강생이 없습니다.</p>
          ) : (
            <>
              <div className="ams-target-picker__actions">
                <button
                  type="button"
                  className="ams-btn ams-btn--ghost ams-btn--sm"
                  disabled={disabled}
                  onClick={selectAll}
                >
                  전체 선택
                </button>
                <button
                  type="button"
                  className="ams-btn ams-btn--ghost ams-btn--sm"
                  disabled={disabled}
                  onClick={clearAll}
                >
                  전체 해제
                </button>
                <span className="ams-target-picker__count">
                  {selectedSet.size}/{students.length}명
                </span>
              </div>
              <ul className="ams-target-picker__list">
                {students.map((s) => (
                  <li key={s.studentId}>
                    <label className="ams-target-picker__item">
                      <input
                        type="checkbox"
                        checked={selectedSet.has(s.studentId)}
                        disabled={disabled}
                        onChange={() => toggleStudent(s.studentId)}
                      />
                      <span>{s.studentName}</span>
                    </label>
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}
    </div>
  )
}
