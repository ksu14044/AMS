import { useState } from 'react'
import { updateClinicReservationResult } from '../api/classesApi'

export default function AssistantClinicSlotExpand({ item, onSaved, onError }) {
  const [resultDraft, setResultDraft] = useState({})
  const [submitting, setSubmitting] = useState(false)

  const reservations = item.reservations ?? []
  const time = item.slot.startTime?.slice(0, 5) ?? ''

  async function handleSaveResult(reservationId) {
    const d = resultDraft[reservationId] || {}
    setSubmitting(true)
    onError('')
    try {
      await updateClinicReservationResult(reservationId, {
        resultAttended: d.attended === '' ? null : d.attended === 'true',
        resultMemo: d.memo || null,
      })
      await onSaved()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="ams-assistant-clinic-home__expand">
      <p className="ams-assistant-clinic-home__expand-head">
        <strong>{time}</strong>
        <span>{item.className}</span>
        <span className="ams-assistant-clinic-home__expand-meta">
          {reservations.length}/{item.maxCapacity}명
        </span>
      </p>

      {reservations.length === 0 ? (
        <p className="ams-class-home__empty">예약된 학생이 없습니다.</p>
      ) : (
        <ul className="ams-clinic-day-panel__students">
          {reservations.map((r) => (
            <li key={r.reservationId} className="ams-clinic-day-panel__student">
              <span className="ams-clinic-day-panel__student-name">{r.studentName}</span>
              <div className="ams-clinic-day-panel__result">
                <select
                  value={
                    resultDraft[r.reservationId]?.attended ??
                    (r.resultAttended == null ? '' : String(r.resultAttended))
                  }
                  onChange={(e) =>
                    setResultDraft((prev) => ({
                      ...prev,
                      [r.reservationId]: {
                        ...prev[r.reservationId],
                        attended: e.target.value,
                      },
                    }))
                  }
                >
                  <option value="">출석 선택</option>
                  <option value="true">출석</option>
                  <option value="false">결석</option>
                </select>
                <input
                  type="text"
                  placeholder="메모"
                  value={resultDraft[r.reservationId]?.memo ?? r.resultMemo ?? ''}
                  onChange={(e) =>
                    setResultDraft((prev) => ({
                      ...prev,
                      [r.reservationId]: {
                        ...prev[r.reservationId],
                        memo: e.target.value,
                      },
                    }))
                  }
                />
                <button
                  type="button"
                  className="ams-btn ams-btn--primary ams-btn--sm"
                  disabled={submitting}
                  onClick={() => handleSaveResult(r.reservationId)}
                >
                  저장
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
