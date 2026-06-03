import { uploadHomeworkAnswerKey, uploadTestAnswerKey } from '../api/classesApi'

export function linkedIdsByType(linkedItems, type) {
  return (linkedItems ?? []).filter((i) => i.type === type).map((i) => i.id)
}

export function newLinkedIds(linkedItems, beforeIds, type) {
  return linkedIdsByType(linkedItems, type).filter((id) => !beforeIds.has(id))
}

export async function uploadAnswerKeysForLessonRecord(
  classId,
  linkedItems,
  { homeworkFile, homeworkQuestionCount, testFile, testQuestionCount },
  beforeIds = { homework: new Set(), test: new Set() },
) {
  const errors = []

  if (homeworkFile) {
    const ids = newLinkedIds(linkedItems, beforeIds.homework, 'homework')
    const homeworkId = ids.length === 1 ? ids[0] : linkedIdsByType(linkedItems, 'homework').at(-1)
    const count = Number(homeworkQuestionCount)
    if (!homeworkId || !count) {
      errors.push('숙제 정답지: 문항 수를 입력한 뒤 업로드하세요.')
    } else {
      try {
        await uploadHomeworkAnswerKey(classId, homeworkId, homeworkFile, count)
      } catch (err) {
        errors.push(err.message || '숙제 정답지 업로드에 실패했습니다.')
      }
    }
  }

  if (testFile) {
    const ids = newLinkedIds(linkedItems, beforeIds.test, 'test')
    const testId = ids.length === 1 ? ids[0] : linkedIdsByType(linkedItems, 'test').at(-1)
    const count = Number(testQuestionCount)
    if (!testId || !count) {
      errors.push('테스트 정답지: 문항 수를 입력한 뒤 업로드하세요.')
    } else {
      try {
        await uploadTestAnswerKey(classId, testId, testFile, count)
      } catch (err) {
        errors.push(err.message || '테스트 정답지 업로드에 실패했습니다.')
      }
    }
  }

  if (errors.length > 0) {
    throw new Error(errors.join(' '))
  }
}
