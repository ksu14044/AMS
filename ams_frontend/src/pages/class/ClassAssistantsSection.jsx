import { useCallback, useEffect, useState } from 'react'
import { fetchClassAssistants, updateClassAssistants } from '../../api/classesApi'
import { roleLabel } from '../../auth/roleLabels'

export default function ClassAssistantsSection({ classId, onError }) {
  const [assigned, setAssigned] = useState([])
  const [available, setAvailable] = useState([])
  const [selected, setSelected] = useState(new Set())
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    onError('')
    try {
      const data = await fetchClassAssistants(classId)
      setAssigned(data.assigned ?? [])
      setAvailable(data.available ?? [])
      setSelected(new Set((data.assigned ?? []).map((a) => a.assistantId)))
    } catch (err) {
      onError(err.message)
    } finally {
      setLoading(false)
    }
  }, [classId, onError])

  useEffect(() => {
    load()
  }, [load])

  function toggleAssistant(assistantId) {
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(assistantId)) {
        next.delete(assistantId)
      } else {
        next.add(assistantId)
      }
      return next
    })
  }

  async function handleSave(e) {
    e.preventDefault()
    setSubmitting(true)
    onError('')
    try {
      const data = await updateClassAssistants(classId, [...selected])
      setAssigned(data.assigned ?? [])
      setAvailable(data.available ?? [])
      setSelected(new Set((data.assigned ?? []).map((a) => a.assistantId)))
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return <p className="ams-class-detail__empty">불러오는 중…</p>
  }

  return (
    <section className="ams-class-detail__section">
      <h3 className="ams-class-detail__heading">조교 담당 반</h3>
      <p className="ams-class-detail__hint-inline">
        이 반을 담당할 조교를 선택합니다. (같은 과목 조교만 표시)
      </p>

      {available.length === 0 ? (
        <p className="ams-class-detail__empty">배정 가능한 조교가 없습니다. 관리자에게 조교 승인을 요청하세요.</p>
      ) : (
        <form className="ams-assistant-pick" onSubmit={handleSave}>
          <ul className="ams-assistant-pick__list">
            {available.map((a) => (
              <li key={a.assistantId} className="ams-assistant-pick__item">
                <label>
                  <input
                    type="checkbox"
                    checked={selected.has(a.assistantId)}
                    onChange={() => toggleAssistant(a.assistantId)}
                    disabled={submitting}
                  />
                  <span className="ams-assistant-pick__name">{a.name}</span>
                  <span className="ams-assistant-pick__role">{roleLabel(a.role)}</span>
                </label>
              </li>
            ))}
          </ul>
          <button type="submit" className="ams-btn ams-btn--primary" disabled={submitting}>
            {submitting ? '저장 중…' : '조교 배정 저장'}
          </button>
        </form>
      )}

      {assigned.length > 0 && (
        <div className="ams-assistant-pick__assigned">
          <h4 className="ams-class-detail__subheading">현재 담당 조교</h4>
          <ul>
            {assigned.map((a) => (
              <li key={a.assignmentId}>{a.assistantName}</li>
            ))}
          </ul>
        </div>
      )}
    </section>
  )
}
