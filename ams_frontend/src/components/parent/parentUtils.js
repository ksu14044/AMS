export const PARENT_TYPE_LABEL = {
  HOMEWORK: '숙제',
  TEST: '테스트',
  VIDEO: '영상',
  CLINIC: '클리닉',
}

export const GAUGE_CLASS = { GREEN: 'green', ORANGE: 'orange', RED: 'red' }

export function nameInitial(name) {
  const trimmed = (name || '').trim()
  if (!trimmed) return '?'
  return trimmed.slice(-2)
}
