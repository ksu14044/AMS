import { useState } from 'react'
import { updateClinicReservationResult } from '../api/classesApi'
import ClinicSlotResultTable from './ClinicSlotResultTable'

export default function AssistantClinicSlotExpand({ item, onSaved, onError }) {
  const [resultDraft, setResultDraft] = useState({})
  const [submitting, setSubmitting] = useState(false)

  const reservations = item.reservations ?? []
  const fields = item.slot?.resultFields ?? []
  const time = item.slot.startTime?.slice(0, 5) ?? ''

  async function handleSaveResult(reservationId, payload) {
    setSubmitting(true)
    onError('')
    try {
      await updateClinicReservationResult(reservationId, payload)
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
        {item.slot?.presetName && <span>{item.slot.presetName}</span>}
        <span className="ams-assistant-clinic-home__expand-meta">
          {reservations.length}/{item.maxCapacity}명
        </span>
      </p>

      {reservations.length === 0 ? (
        <p className="ams-class-home__empty">예약된 학생이 없습니다.</p>
      ) : (
        <ClinicSlotResultTable
          fields={fields}
          reservations={reservations}
          resultDraft={resultDraft}
          onDraftChange={(reservationId, next) =>
            setResultDraft((prev) => ({ ...prev, [reservationId]: next }))
          }
          onSaveResult={handleSaveResult}
          submitting={submitting}
        />
      )}
    </div>
  )
}
