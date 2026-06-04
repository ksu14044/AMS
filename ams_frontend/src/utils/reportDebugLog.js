const PREFIX = '[AMS Report]'

/** @param {string} action */
export function logReportError(action, details = {}) {
  console.error(PREFIX, action, {
    at: new Date().toISOString(),
    ...details,
  })
}

/** @param {string} action */
export function logReportWarn(action, details = {}) {
  console.warn(PREFIX, action, {
    at: new Date().toISOString(),
    ...details,
  })
}

/** @param {string} action */
export function logReportInfo(action, details = {}) {
  console.info(PREFIX, action, {
    at: new Date().toISOString(),
    ...details,
  })
}
