/**
 * 최근에 본 반(classId) 추적 — 학생 홈에서 "최근 본 반" 카드를 강조하기 위한 보조 유틸.
 * 백엔드 의존 없이 localStorage 만으로 동작하므로 Phase 10.1 디자인 정합 슬라이스에서 사용.
 */
import { useEffect, useState } from 'react'

const STORAGE_KEY = 'ams.lastClassId'
const CHANGE_EVENT = 'ams:last-class-changed'

function safeGet() {
  try {
    return window.localStorage.getItem(STORAGE_KEY)
  } catch {
    return null
  }
}

export function rememberLastClass(classId) {
  if (classId == null) return
  try {
    window.localStorage.setItem(STORAGE_KEY, String(classId))
    window.dispatchEvent(new Event(CHANGE_EVENT))
  } catch {
    /* sandbox/private mode 등 — 무시 */
  }
}

export function clearLastClass() {
  try {
    window.localStorage.removeItem(STORAGE_KEY)
    window.dispatchEvent(new Event(CHANGE_EVENT))
  } catch {
    /* no-op */
  }
}

/**
 * 컴포넌트 간에 최근 반 ID를 공유하기 위한 훅.
 * - 다른 페이지/탭에서 갱신되어도 `storage` 이벤트로 동기화된다.
 */
export function useLastClassId() {
  const [lastClassId, setLastClassId] = useState(safeGet)

  useEffect(() => {
    function sync() {
      setLastClassId(safeGet())
    }
    window.addEventListener('storage', sync)
    window.addEventListener(CHANGE_EVENT, sync)
    return () => {
      window.removeEventListener('storage', sync)
      window.removeEventListener(CHANGE_EVENT, sync)
    }
  }, [])

  return [lastClassId, setLastClassId]
}
