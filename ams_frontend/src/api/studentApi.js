import { fetchClasses } from './classesApi'

/** @deprecated {@link fetchClasses} 사용 권장 */
export function fetchMyClasses() {
  return fetchClasses()
}
