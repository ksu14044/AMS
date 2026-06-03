import { apiRequest, fetchWithAuth } from './client'

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

async function downloadBlob(url, filename, errorMessage, options = {}) {
  const response = await fetchWithAuth(url, options)
  if (!response.ok) {
    throw new Error(errorMessage)
  }
  const blob = await response.blob()
  const objectUrl = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = objectUrl
  a.download = filename
  a.click()
  URL.revokeObjectURL(objectUrl)
}

export function downloadReportPdf(reportId) {
  return downloadBlob(
    `${API_BASE}/reports/${reportId}/pdf`,
    `diligence-report-${reportId}.pdf`,
    'PDF 다운로드에 실패했습니다.',
  )
}

export async function downloadPeriodReportsArchive(classId, periodStart, periodEnd, zipFilename) {
  const response = await fetchWithAuth(`${API_BASE}/classes/${classId}/reports/archive`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ periodStart, periodEnd }),
  })
  if (!response.ok) {
    throw new Error('PDF 일괄 다운로드에 실패했습니다.')
  }
  const blob = await response.blob()
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
