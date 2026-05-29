import { apiRequest } from './client'

/** 조교: 담당 반 배정 없이 clinic_slot.assistant_id로 배정된 주간 클리닉 */
export function fetchMyAssistantClinicWeek(weekStart) {
  return apiRequest(`/me/clinic/weeks/${weekStart}`)
}
