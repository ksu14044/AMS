import { useState } from 'react'
import {
  CLINIC_FIELD_TYPES,
  DEFAULT_CLINIC_FIELDS,
} from '../utils/clinicPresets'

function emptyField() {
  return { key: '', label: '', type: 'text', required: false, maxLength: '', optionsText: '' }
}

function fieldsFromPreset(preset) {
  return (preset?.fields ?? DEFAULT_CLINIC_FIELDS).map((field) => ({
    key: field.key ?? '',
    label: field.label ?? '',
    type: field.type ?? 'text',
    required: Boolean(field.required),
    maxLength: field.maxLength != null ? String(field.maxLength) : '',
    optionsText: (field.options ?? []).join(', '),
  }))
}

function fieldsToPayload(fields) {
  return fields.map((field) => {
    const payload = {
      key: field.key.trim(),
      label: field.label.trim(),
      type: field.type,
      required: Boolean(field.required),
    }
    if (field.type === 'text' && field.maxLength) {
      payload.maxLength = Number(field.maxLength)
    }
    if (field.type === 'select') {
      payload.options = field.optionsText
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean)
    }
    return payload
  })
}

function PresetEditor({ initialName, initialFields, submitting, onCancel, onSave }) {
  const [name, setName] = useState(initialName)
  const [fields, setFields] = useState(initialFields)

  function updateField(index, patch) {
    setFields((prev) => prev.map((f, i) => (i === index ? { ...f, ...patch } : f)))
  }

  return (
    <form
      className="ams-clinic-preset-editor"
      onSubmit={(e) => {
        e.preventDefault()
        onSave(name.trim(), fieldsToPayload(fields))
      }}
    >
      <label className="ams-field ams-field--compact">
        <span className="ams-field__label">프리셋 이름</span>
        <input
          className="ams-field__input"
          value={name}
          onChange={(e) => setName(e.target.value)}
          maxLength={100}
          required
        />
      </label>
      <div className="ams-clinic-preset-editor__fields">
        <p className="ams-class-detail__hint-inline">결과 입력 필드</p>
        {fields.map((field, index) => (
          <div key={index} className="ams-clinic-preset-editor__row">
            <input
              placeholder="key (예: attended)"
              value={field.key}
              onChange={(e) => updateField(index, { key: e.target.value })}
              required
            />
            <input
              placeholder="라벨"
              value={field.label}
              onChange={(e) => updateField(index, { label: e.target.value })}
              required
            />
            <select
              value={field.type}
              onChange={(e) => updateField(index, { type: e.target.value })}
            >
              {CLINIC_FIELD_TYPES.map((t) => (
                <option key={t.value} value={t.value}>
                  {t.label}
                </option>
              ))}
            </select>
            <label className="ams-clinic-preset-editor__check">
              <input
                type="checkbox"
                checked={field.required}
                onChange={(e) => updateField(index, { required: e.target.checked })}
              />
              필수
            </label>
            {field.type === 'text' && (
              <input
                type="number"
                min={1}
                placeholder="최대 길이"
                value={field.maxLength}
                onChange={(e) => updateField(index, { maxLength: e.target.value })}
              />
            )}
            {field.type === 'select' && (
              <input
                placeholder="옵션 (쉼표 구분)"
                value={field.optionsText}
                onChange={(e) => updateField(index, { optionsText: e.target.value })}
              />
            )}
            <button
              type="button"
              className="ams-btn ams-btn--ghost ams-btn--sm"
              disabled={fields.length <= 1}
              onClick={() => setFields((prev) => prev.filter((_, i) => i !== index))}
            >
              삭제
            </button>
          </div>
        ))}
        <button
          type="button"
          className="ams-btn ams-btn--ghost ams-btn--sm"
          onClick={() => setFields((prev) => [...prev, emptyField()])}
        >
          + 필드 추가
        </button>
      </div>
      <div className="ams-clinic-preset-editor__actions">
        <button type="submit" className="ams-btn ams-btn--primary ams-btn--sm" disabled={submitting}>
          저장
        </button>
        <button type="button" className="ams-btn ams-btn--ghost ams-btn--sm" onClick={onCancel}>
          취소
        </button>
      </div>
    </form>
  )
}

export default function ClinicPresetSection({
  presets,
  canManage,
  submitting,
  onCreate,
  onUpdate,
  onDelete,
}) {
  const [creating, setCreating] = useState(false)
  const [editingId, setEditingId] = useState(null)

  if (!canManage) return null

  return (
    <section className="ams-clinic-preset-section">
      <div className="ams-clinic-preset-section__head">
        <h4 className="ams-class-detail__heading">결과 프리셋</h4>
        {!creating && !editingId && (
          <button
            type="button"
            className="ams-btn ams-btn--ghost ams-btn--sm"
            onClick={() => setCreating(true)}
          >
            + 프리셋 추가
          </button>
        )}
      </div>
      <p className="ams-class-detail__hint-inline">
        클리닉 슬롯 등록 시 선택합니다. 종료 후 아래 양식으로 결과를 입력합니다.
      </p>

      {creating && (
        <PresetEditor
          initialName=""
          initialFields={fieldsFromPreset({ fields: DEFAULT_CLINIC_FIELDS })}
          submitting={submitting}
          onCancel={() => setCreating(false)}
          onSave={async (name, fields) => {
            await onCreate({ name, fields })
            setCreating(false)
          }}
        />
      )}

      <ul className="ams-clinic-preset-section__list">
        {presets.map((preset) => (
          <li key={preset.presetId} className="ams-clinic-preset-section__item">
            {editingId === preset.presetId ? (
              <PresetEditor
                initialName={preset.name}
                initialFields={fieldsFromPreset(preset)}
                submitting={submitting}
                onCancel={() => setEditingId(null)}
                onSave={async (name, fields) => {
                  await onUpdate(preset.presetId, { name, fields })
                  setEditingId(null)
                }}
              />
            ) : (
              <>
                <div className="ams-clinic-preset-section__summary">
                  <strong>{preset.name}</strong>
                  <span>{preset.fields?.length ?? 0}개 필드</span>
                </div>
                <div className="ams-clinic-preset-section__actions">
                  <button
                    type="button"
                    className="ams-btn ams-btn--ghost ams-btn--sm"
                    disabled={submitting}
                    onClick={() => setEditingId(preset.presetId)}
                  >
                    수정
                  </button>
                  <button
                    type="button"
                    className="ams-btn ams-btn--ghost ams-btn--sm"
                    disabled={submitting || presets.length <= 1}
                    onClick={() => onDelete(preset.presetId, preset.name)}
                  >
                    삭제
                  </button>
                </div>
              </>
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}

export function ClinicPresetPicker({ presets, value, onChange, disabled, required = true }) {
  if (!presets?.length) {
    return <p className="ams-class-detail__empty">프리셋을 불러오는 중…</p>
  }
  return (
    <label className="ams-field ams-field--compact">
      <span className="ams-field__label">결과 프리셋</span>
      <select
        className="ams-field__input"
        value={value}
        onChange={onChange}
        required={required}
        disabled={disabled}
      >
        <option value="" disabled>
          프리셋 선택
        </option>
        {presets.map((p) => (
          <option key={p.presetId} value={p.presetId}>
            {p.name}
          </option>
        ))}
      </select>
    </label>
  )
}
