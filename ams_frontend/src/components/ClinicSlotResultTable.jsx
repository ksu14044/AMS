import { useState } from 'react'
import { buildResultPayload, draftFromReservation } from '../utils/clinicPresets'

function formatReadonlyValue(field, raw) {
  if (raw == null || raw === '') return '—'
  if (field.type === 'boolean') return raw ? '출석' : '결석'
  return String(raw)
}

function columnWidth(field) {
  if (field.type === 'boolean') return 80
  if (field.type === 'number') return 84
  if (field.type === 'select') return 120
  return 176
}

function hasMultilineField(fields) {
  return fields.some((f) => f.type === 'text')
}

function ReadonlyCell({ field, value }) {
  const [expanded, setExpanded] = useState(false)
  const text = formatReadonlyValue(field, value)
  const isLongText = field.type === 'text' && text.length > 36

  if (!isLongText) {
    return (
      <span className="ams-clinic-result-table__readonly" title={text}>
        {text}
      </span>
    )
  }

  return (
    <div
      className={
        expanded
          ? 'ams-clinic-result-table__readonly ams-clinic-result-table__readonly--expanded'
          : 'ams-clinic-result-table__readonly ams-clinic-result-table__readonly--clamp'
      }
      title={!expanded ? text : undefined}
    >
      {text}
      <button
        type="button"
        className="ams-clinic-result-table__expand"
        onClick={() => setExpanded((v) => !v)}
      >
        {expanded ? '접기' : '펼치기'}
      </button>
    </div>
  )
}

function ResultFieldInput({ field, value, onChange, disabled }) {
  if (field.type === 'boolean') {
    return (
      <select
        className="ams-clinic-result-table__input ams-clinic-result-table__input--compact"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-label={field.label}
        disabled={disabled}
      >
        <option value="">—</option>
        <option value="true">출석</option>
        <option value="false">결석</option>
      </select>
    )
  }
  if (field.type === 'select') {
    return (
      <select
        className="ams-clinic-result-table__input ams-clinic-result-table__input--compact"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-label={field.label}
        disabled={disabled}
      >
        <option value="">—</option>
        {(field.options ?? []).map((opt) => (
          <option key={opt} value={opt}>
            {opt}
          </option>
        ))}
      </select>
    )
  }
  if (field.type === 'number') {
    return (
      <input
        className="ams-clinic-result-table__input ams-clinic-result-table__input--compact"
        type="number"
        placeholder={field.label}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-label={field.label}
        disabled={disabled}
      />
    )
  }
  return (
    <textarea
      className="ams-clinic-result-table__input ams-clinic-result-table__textarea"
      rows={2}
      placeholder={field.label}
      maxLength={field.maxLength ?? undefined}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      aria-label={field.label}
      disabled={disabled}
    />
  )
}

export default function ClinicSlotResultTable({
  fields,
  reservations,
  resultDraft,
  onDraftChange,
  onSaveResult,
  submitting,
  editable = true,
}) {
  if (!fields?.length || !reservations?.length) return null

  const multiline = hasMultilineField(fields)
  const nameWidth = 100
  const actionWidth = 52
  const tableMinWidth =
    nameWidth +
    fields.reduce((sum, f) => sum + columnWidth(f), 0) +
    (editable ? actionWidth : 0)

  return (
    <div className="ams-clinic-result-table-wrap">
      <table
        className={
          multiline
            ? 'ams-clinic-result-table ams-clinic-result-table--multiline'
            : 'ams-clinic-result-table'
        }
        style={{ minWidth: tableMinWidth }}
      >
        <colgroup>
          <col style={{ width: nameWidth }} />
          {fields.map((field) => (
            <col key={field.key} style={{ width: columnWidth(field) }} />
          ))}
          {editable && <col style={{ width: actionWidth }} />}
        </colgroup>
        <thead>
          <tr>
            <th className="ams-clinic-result-table__col-name">학생</th>
            {fields.map((field) => (
              <th key={field.key} className="ams-clinic-result-table__col-field">
                {field.label}
                {field.required && <span className="ams-clinic-result-table__req">*</span>}
              </th>
            ))}
            {editable && <th className="ams-clinic-result-table__col-action">저장</th>}
          </tr>
        </thead>
        <tbody>
          {reservations.map((reservation) => {
            const draft =
              resultDraft?.[reservation.reservationId] ??
              draftFromReservation(fields, reservation)
            const saved = Boolean(reservation.resultSavedAt)

            return (
              <tr
                key={reservation.reservationId}
                className={saved ? 'ams-clinic-result-table__row--saved' : undefined}
              >
                <td className="ams-clinic-result-table__name">
                  <span
                    className={
                      saved
                        ? 'ams-clinic-result-table__status ams-clinic-result-table__status--done'
                        : 'ams-clinic-result-table__status ams-clinic-result-table__status--pending'
                    }
                    title={saved ? '결과 입력 완료' : '미입력'}
                    aria-hidden
                  />
                  <span className="ams-clinic-result-table__name-text">{reservation.studentName}</span>
                </td>
                {fields.map((field) => {
                  const value = draft?.[field.key] ?? ''
                  return (
                    <td key={field.key} className="ams-clinic-result-table__cell">
                      {editable ? (
                        <ResultFieldInput
                          field={field}
                          value={value}
                          disabled={submitting}
                          onChange={(next) =>
                            onDraftChange(reservation.reservationId, {
                              ...draft,
                              [field.key]: next,
                            })
                          }
                        />
                      ) : (
                        <ReadonlyCell field={field} value={reservation.result?.[field.key]} />
                      )}
                    </td>
                  )
                })}
                {editable && (
                  <td className="ams-clinic-result-table__action">
                    <button
                      type="button"
                      className="ams-btn ams-btn--primary ams-btn--sm ams-clinic-result-table__save"
                      disabled={submitting}
                      onClick={() =>
                        onSaveResult(
                          reservation.reservationId,
                          buildResultPayload(fields, draft),
                        )
                      }
                    >
                      저장
                    </button>
                  </td>
                )}
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
