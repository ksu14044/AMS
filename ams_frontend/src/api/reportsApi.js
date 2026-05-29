import { apiRequest, fetchWithAuth } from './client'

const API_BASE = '/api/v1'

export function fetchClassReports(classId) {
  return apiRequest(`/classes/${classId}/reports`)
}

export function fetchReportGenerationTargets(classId) {
  return apiRequest(`/classes/${classId}/reports/generation-targets`)
}

export function generateClassReports(classId, testId) {
  return apiRequest(`/classes/${classId}/reports/generate/${testId}`, {
    method: 'POST',
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

async function downloadBlob(url, filename, errorMessage) {
  const response = await fetchWithAuth(url)
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

export function downloadTestReportsArchive(classId, testId, zipFilename) {
  return downloadBlob(
    `${API_BASE}/classes/${classId}/reports/tests/${testId}/pdf-archive`,
    zipFilename || `reports-test-${testId}.zip`,
    'PDF 일괄 다운로드에 실패했습니다.',
  )
}

export function formatReportPeriod(start, end) {
  const fmt = (iso) => {
    const d = new Date(iso)
    return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
  }
  return `${fmt(start)} ~ ${fmt(end)}`
}
