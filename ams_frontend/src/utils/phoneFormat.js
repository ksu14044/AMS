/** 숫자만 추출 */
export function phoneDigitsOnly(value) {
  return (value ?? '').replace(/\D/g, '')
}

/**
 * 한국 전화번호 표시 형식 (010-1234-5678, 02-123-4567 등)
 */
export function formatPhoneNumber(value) {
  const d = phoneDigitsOnly(value)
  if (!d) return ''

  if (d.startsWith('02')) {
    if (d.length <= 2) return d
    if (d.length <= 5) return `${d.slice(0, 2)}-${d.slice(2)}`
    if (d.length <= 9) return `${d.slice(0, 2)}-${d.slice(2, 5)}-${d.slice(5)}`
    return `${d.slice(0, 2)}-${d.slice(2, 6)}-${d.slice(6, 10)}`
  }

  if (/^01[016789]/.test(d)) {
    if (d.length <= 3) return d
    if (d.length <= 7) return `${d.slice(0, 3)}-${d.slice(3)}`
    return `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7, 11)}`
  }

  if (d.startsWith('0')) {
    if (d.length <= 3) return d
    if (d.length <= 6) return `${d.slice(0, 3)}-${d.slice(3)}`
    if (d.length <= 10) return `${d.slice(0, 3)}-${d.slice(3, 6)}-${d.slice(6)}`
    return `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7, 11)}`
  }

  if (d.length <= 4) return d
  if (d.length <= 8) return `${d.slice(0, 4)}-${d.slice(4)}`
  return `${d.slice(0, 4)}-${d.slice(4, 8)}-${d.slice(8, 12)}`
}

/** 입력 중 자동 하이픈 (최대 11자리 휴대폰 기준) */
export function formatPhoneInput(value) {
  const d = phoneDigitsOnly(value).slice(0, 11)
  return formatPhoneNumber(d)
}
