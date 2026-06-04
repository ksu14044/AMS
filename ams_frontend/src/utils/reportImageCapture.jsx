import { createRoot } from 'react-dom/client'
import { toBlob } from 'html-to-image'
import ReportDetailContent from '../components/ReportDetailContent'
import { fetchReportDetail, uploadReportImage } from '../api/reportsApi'

const CAPTURE_OPTIONS = {
  pixelRatio: 1,
  cacheBust: false,
  skipFonts: true,
  backgroundColor: '#ffffff',
}

const BATCH_CONCURRENCY = 3

let captureHost = null
let captureRoot = null

function waitForPaint() {
  return new Promise((resolve) => {
    requestAnimationFrame(() => requestAnimationFrame(resolve))
  })
}

function ensureCaptureRoot() {
  if (!captureHost) {
    captureHost = document.createElement('div')
    captureHost.className = 'ams-report-capture-host'
    document.body.appendChild(captureHost)
    captureRoot = createRoot(captureHost)
  }
  return captureRoot
}

export async function captureReportPngBlob(detail) {
  const root = ensureCaptureRoot()
  root.render(<ReportDetailContent detail={detail} captureMode />)
  await waitForPaint()
  const node = captureHost.querySelector('.ams-report-modal--capture')
  if (!node) {
    throw new Error('보고서 화면을 렌더링하지 못했습니다.')
  }
  const blob = await toBlob(node, CAPTURE_OPTIONS)
  if (!blob) {
    throw new Error('PNG 생성에 실패했습니다.')
  }
  return blob
}

export async function refreshReportImage(reportId, detail) {
  const blob = await captureReportPngBlob(detail)
  await uploadReportImage(reportId, blob)
}

async function runWithConcurrency(items, limit, fn) {
  let index = 0
  async function worker() {
    while (index < items.length) {
      const i = index++
      await fn(items[i], i)
    }
  }
  const workers = Math.min(limit, items.length)
  if (workers === 0) return
  await Promise.all(Array.from({ length: workers }, () => worker()))
}

export async function captureAndUploadReportImages(reportIds, { onProgress, concurrency = BATCH_CONCURRENCY } = {}) {
  const ids = [...reportIds]
  let completed = 0
  await runWithConcurrency(ids, concurrency, async (reportId) => {
    const detail = await fetchReportDetail(reportId)
    await refreshReportImage(reportId, detail)
    completed += 1
    onProgress?.(completed, ids.length, reportId)
  })
}
