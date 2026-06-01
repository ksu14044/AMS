export const DEFAULT_CLINIC_FIELDS = [
  { key: 'attended', label: '참석', type: 'boolean', required: true },
  { key: 'memo', label: '메모', type: 'text', maxLength: 500 },
]

export const CLINIC_FIELD_TYPES = [
  { value: 'boolean', label: '예/아니오' },
  { value: 'text', label: '텍스트' },
  { value: 'number', label: '숫자' },
  { value: 'select', label: '선택' },
]

export function defaultPresetId(presets) {
  if (!presets?.length) return ''
  const basic = presets.find((p) => p.name === '기본')
  return String((basic ?? presets[0]).presetId)
}

export function formatClinicResultReadonly(fields, result) {
  if (!fields?.length) {
    if (result?.attended == null) return '미입력'
    const attended = result.attended ? '출석' : '결석'
    return result.memo ? `${attended} · ${result.memo}` : attended
  }
  const parts = fields
    .map((field) => {
      const raw = result?.[field.key]
      if (raw == null || raw === '') return null
      if (field.type === 'boolean') return `${field.label}: ${raw ? '예' : '아니오'}`
      return `${field.label}: ${raw}`
    })
    .filter(Boolean)
  return parts.length ? parts.join(' · ') : '미입력'
}

export function buildResultPayload(fields, draft) {
  const result = {}
  for (const field of fields ?? []) {
    const raw = draft?.[field.key]
    if (field.type === 'boolean') {
      if (raw === '' || raw == null) continue
      result[field.key] = raw === true || raw === 'true'
    } else if (raw != null && raw !== '') {
      result[field.key] = field.type === 'number' ? Number(raw) : raw
    }
  }
  return { result }
}

export function draftFromReservation(fields, reservation) {
  const draft = {}
  const source = reservation?.result ?? {}
  for (const field of fields ?? []) {
    const raw = source[field.key]
    if (field.type === 'boolean') {
      draft[field.key] = raw == null ? '' : String(raw)
    } else {
      draft[field.key] = raw ?? ''
    }
  }
  return draft
}
