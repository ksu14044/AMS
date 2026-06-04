/**
 * 클립보드 복사 — HTTPS(Clipboard API) · HTTP·구형 브라우저(execCommand) 폴백
 * @param {string} text
 * @param {HTMLInputElement | HTMLTextAreaElement | null} [inputElement] readonly input이 있으면 우선 사용
 */
export async function copyTextToClipboard(text, inputElement) {
  const value = String(text ?? '')
  if (!value) return false

  if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText && window.isSecureContext) {
    try {
      await navigator.clipboard.writeText(value)
      return true
    } catch {
      /* execCommand 폴백 */
    }
  }

  if (inputElement instanceof HTMLInputElement || inputElement instanceof HTMLTextAreaElement) {
    if (copyFromInput(inputElement, value)) {
      return true
    }
  }

  return copyFromHiddenTextarea(value)
}

function copyFromInput(el, value) {
  try {
    const wasReadOnly = el.readOnly
    el.readOnly = false
    el.focus()
    el.value = value
    el.select()
    el.setSelectionRange(0, value.length)
    const ok = document.execCommand('copy')
    el.readOnly = wasReadOnly
    return ok
  } catch {
    return false
  }
}

function copyFromHiddenTextarea(value) {
  const textarea = document.createElement('textarea')
  textarea.value = value
  textarea.setAttribute('readonly', '')
  Object.assign(textarea.style, {
    position: 'fixed',
    top: '0',
    left: '0',
    width: '1px',
    height: '1px',
    padding: '0',
    border: 'none',
    outline: 'none',
    opacity: '0',
  })
  document.body.appendChild(textarea)
  try {
    textarea.focus()
    textarea.select()
    textarea.setSelectionRange(0, value.length)
    return document.execCommand('copy')
  } catch {
    return false
  } finally {
    document.body.removeChild(textarea)
  }
}
