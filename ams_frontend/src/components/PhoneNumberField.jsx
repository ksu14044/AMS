import { formatPhoneInput } from '../utils/phoneFormat'

export default function PhoneNumberField({ value, onChange, required = true, id = 'phoneNumber' }) {
  return (
    <label className="ams-field">
      <span>전화번호</span>
      <input
        id={id}
        type="tel"
        inputMode="numeric"
        autoComplete="tel"
        value={value}
        onChange={(e) => onChange(formatPhoneInput(e.target.value))}
        placeholder="010-0000-0000"
        required={required}
        maxLength={13}
      />
    </label>
  )
}
