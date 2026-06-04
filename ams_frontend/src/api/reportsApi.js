import { apiRequest, fetchWithAuth } from './client'
import { logReportError } from '../utils/reportDebugLog'

const API_BASE = '/api/v1'

export function fetchClassReports(classId) {
  return apiRequest(`/classes/${classId}/reports`)
}

export function fetchReportPeriodPresets(classId) {
  return apiRequest(`/classes/${classId}/report-period-presets`)
}

export function createReportPeriodPreset(classId, body) {
  return apiRequest(`/classes/${classId}/report-period-presets`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function updateReportPeriodPreset(classId, presetId, body) {
  return apiRequest(`/classes/${classId}/report-period-presets/${presetId}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export function deleteReportPeriodPreset(classId, presetId) {
  return apiRequest(`/classes/${classId}/report-period-presets/${presetId}`, {
    method: 'DELETE',
  })
}

/** v3.0: 기간·학생 선택 보고서 생성 */
export function generateReports(classId, body) {
  return apiRequest(`/classes/${classId}/reports/generate`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function fetchReportDetail(reportId) {
  return apiRequest(`/reports/${reportId}`)
}

export function updateReportComment(reportId, comment) {
  return apiRequest(`/reports/${reportId}/comment`, {
    method: 'PATCH',
    body: JSON.stringify({ comment }),
  })
}

async function readErrorBody(response, fallback) {
  const contentType = response.headers.get('Content-Type') || ''
  if (!contentType.includes('application/json')) {
    return { message: fallback, code: null, body: null }
  }
  const body = await response.json().catch(() => ({}))
  return {
    message: body.message || fallback,
    code: body.code ?? null,
    body,
  }
}

async function downloadBlob(url, filename, errorMessage, options = {}) {
  const response = await fetchWithAuth(url, options)
  if (!response.ok) {
    const { message, code, body } = await readErrorBody(response, errorMessage)
    logReportError('download failed', {
      url,
      filename,
      status: response.status,
      statusText: response.statusText,
      contentType: response.headers.get('Content-Type'),
      code,
      message,
      body,
    })
    throw new Error(message)
  }
  const blob = await response.blob()
  if (blob.size === 0) {
    logReportError('download empty blob', { url, filename, status: response.status })
    throw new Error('다운로드한 파일이 비어 있습니다.')
  }
  const objectUrl = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = objectUrl
  a.download = filename
  a.click()
  URL.revokeObjectURL(objectUrl)
}

export function reportImageUrl(reportId) {
  return `${API_BASE}/reports/${reportId}/image`
}

export function downloadReportImage(reportId) {
  return downloadBlob(
    reportImageUrl(reportId),
    `diligence-report-${reportId}.png`,
    '보고서 다운로드에 실패했습니다.',
  )
}

/** @deprecated 구 경로 호환 — `/image`와 동일 */
export function downloadReportPdf(reportId) {
  return downloadBlob(
    `${API_BASE}/reports/${reportId}/pdf`,
    `diligence-report-${reportId}.png`,
    '보고서 다운로드에 실패했습니다.',
  )
}

function asPngBlob(blob) {
  if (blob.type === 'image/png') {
    return blob
  }
  return new Blob([blob], { type: 'image/png' })
}

export async function uploadReportImage(reportId, blob) {
  const pngBlob = asPngBlob(blob)
  const formData = new FormData()
  formData.append('file', pngBlob, `report-${reportId}.png`)
  const url = `${API_BASE}/reports/${reportId}/image`
  const response = await fetchWithAuth(url, {
    method: 'POST',
    body: formData,
  })
  const body = await response.json().catch(() => ({}))
  if (!response.ok || body.success === false) {
    logReportError('upload failed', {
      url,
      reportId,
      status: response.status,
      statusText: response.statusText,
      blobSize: pngBlob.size,
      blobType: pngBlob.type,
      code: body.code ?? null,
      message: body.message,
      body,
    })
    throw new Error(body.message || '보고서 이미지 업로드에 실패했습니다.')
  }
  return body.data
}

export async function downloadPeriodReportsArchive(classId, periodStart, periodEnd, zipFilename) {
  const url = `${API_BASE}/classes/${classId}/reports/archive`
  const response = await fetchWithAuth(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ periodStart, periodEnd }),
  })
  if (!response.ok) {
    const { message, code, body } = await readErrorBody(response, 'PNG 일괄 다운로드에 실패했습니다.')
    logReportError('archive download failed', {
      url,
      classId,
      periodStart,
      periodEnd,
      status: response.status,
      statusText: response.statusText,
      code,
      message,
      body,
    })
    throw new Error(message)
  }
  const blob = await response.blob()
  if (blob.size === 0) {
    logReportError('archive empty blob', { url, classId, periodStart, periodEnd })
    throw new Error('ZIP 파일이 비어 있습니다.')
  }
  const objectUrl = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = objectUrl
  a.download = zipFilename || 'reports.zip'
  a.click()
  URL.revokeObjectURL(objectUrl)
}

export function formatReportPeriod(start, end) {
  const fmt = (iso) => {
    const d = new Date(iso)
    return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
  }
  return `${fmt(start)} ~ ${fmt(end)}`
}

export function formatLocalDate(dateStr) {
  if (!dateStr) return ''
  const [y, m, d] = dateStr.split('-')
  return `${y}.${m}.${d}`
}
