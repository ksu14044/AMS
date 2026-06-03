import { useMemo, useState } from 'react'
import { fetchWithAuth } from '../api/client'

const API_BASE = '/api/v1'

export const ANSWER_KEY_FILE_ACCEPT = '.pdf,.doc,.docx,.hwp,.hwpx,image/*'

/** 수업기록·숙제·테스트 등록 폼용 정답지 파일 선택 */
export function AnswerKeyFileField({ file, onFileChange, disabled, compact }) {
  return (
    <label className={`ams-field${compact ? ' ams-field--compact' : ''} ams-field--file`}>
      <span className="ams-field__label">정답지 파일 (선택)</span>
      <input
        className="ams-field__input"
        type="file"
        accept={ANSWER_KEY_FILE_ACCEPT}
        disabled={disabled}
        onChange={(e) => onFileChange(e.target.files?.[0] ?? null)}
      />
      <p className="ams-class-detail__hint-inline">
        PDF·이미지·워드·한글. 워드·한글은 텍스트 기준 PDF로 저장됩니다.
      </p>
      {file && (
        <p className="ams-lesson-board__field-hint">
          선택: {file.name}
        </p>
      )}
    </label>
  )
}

function ModalBackdrop({ wide, label, onClose, children }) {
  return (
    <div className="ams-homework-modal-backdrop" onClick={onClose}>
      <div
        className={`ams-homework-modal${wide ? ' ams-homework-modal--wide' : ''}`}
        role="dialog"
        aria-modal="true"
        aria-label={label}
        onClick={(e) => e.stopPropagation()}
      >
        {children}
      </div>
    </div>
  )
}

export function AnswerKeyUploadModal({
  title,
  questionCount,
  hasAnswerKeyFile,
  submitting,
  onQuestionCountChange,
  onUpload,
  onViewPdf,
  onClose,
}) {
  const [file, setFile] = useState(null)

  async function handleSubmit(e) {
    e.preventDefault()
    if (!file) return
    await onUpload(file, questionCount)
    setFile(null)
  }

  return (
    <ModalBackdrop label="정답지 업로드" onClose={onClose}>
      <header className="ams-homework-modal__header">
        <h4 className="ams-homework-modal__title">정답지 업로드</h4>
        <button type="button" className="ams-homework-modal__close" onClick={onClose} aria-label="닫기">
          ×
        </button>
      </header>
      <p className="ams-homework-modal__meta">{title}</p>

      <form className="ams-homework-modal__body" onSubmit={handleSubmit}>
        <label className="ams-homework-modal__count">
          문항 수
          <input
            type="number"
            min={1}
            value={questionCount || ''}
            disabled={submitting}
            onChange={(e) => onQuestionCountChange(e.target.value)}
            required
          />
        </label>

        <label className="ams-homework-modal__field ams-homework-modal__field--file">
          <span>정답지 파일 (PDF·이미지·워드·한글 → PDF 저장)</span>
          <input
            type="file"
            accept={ANSWER_KEY_FILE_ACCEPT}
            disabled={submitting}
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          />
        </label>
        <p className="ams-class-detail__hint-inline">
          워드·한글은 텍스트 기준으로 PDF로 변환됩니다. 표·그림이 많으면 한글/워드에서 PDF로 저장 후 업로드하는 것을 권장합니다.
        </p>

        {hasAnswerKeyFile && (
          <button type="button" className="ams-btn ams-btn--ghost" disabled={submitting} onClick={onViewPdf}>
            등록된 정답지 보기
          </button>
        )}

        <footer className="ams-homework-modal__footer">
          <button type="button" className="ams-btn ams-btn--ghost" disabled={submitting} onClick={onClose}>
            취소
          </button>
          <button
            type="submit"
            className="ams-btn ams-btn--primary"
            disabled={submitting || !file || questionCount <= 0}
          >
            {submitting ? '업로드 중…' : hasAnswerKeyFile ? '정답지 교체' : '정답지 업로드'}
          </button>
        </footer>
      </form>
    </ModalBackdrop>
  )
}

function wrongSetFromRow(row) {
  return new Set(row?.wrongQuestionNos ?? [])
}

function resultDirty(saved, wrongSet) {
  const savedWrong = wrongSetFromRow(saved)
  if (savedWrong.size !== wrongSet.size) return true
  for (const n of wrongSet) {
    if (!savedWrong.has(n)) return true
  }
  return false
}

function computeScorePercent(questionCount, correctCount) {
  if (questionCount <= 0) return null
  const ratio = (100 / questionCount) * correctCount
  return Math.ceil(ratio * 10) / 10
}

export function SubmissionResultModal({
  studentName,
  assignmentTitle,
  questionCount,
  savedRow,
  canManage,
  saving,
  onSave,
  onClose,
}) {
  const [wrongSet, setWrongSet] = useState(() => wrongSetFromRow(savedRow))

  const wrongCount = wrongSet.size
  const correctCount = questionCount - wrongCount

  const dirty = canManage && resultDirty(savedRow, wrongSet)

  const previewScore = useMemo(
    () => computeScorePercent(questionCount, correctCount),
    [questionCount, correctCount],
  )

  function toggleWrong(no) {
    setWrongSet((prev) => {
      const next = new Set(prev)
      if (next.has(no)) next.delete(no)
      else next.add(no)
      return next
    })
  }

  async function handleSave() {
    await onSave([...wrongSet].sort((a, b) => a - b))
  }

  return (
    <ModalBackdrop wide label={`${studentName} 결과`} onClose={onClose}>
      <header className="ams-homework-modal__header">
        <h4 className="ams-homework-modal__title">{canManage ? '결과 입력' : '내 결과'}</h4>
        <button type="button" className="ams-homework-modal__close" onClick={onClose} aria-label="닫기">
          ×
        </button>
      </header>
      <p className="ams-homework-modal__meta">
        {studentName} · {assignmentTitle}
      </p>

      <div className="ams-homework-modal__body">
        <p className="ams-class-detail__meta">
          {canManage ? (
            <>
              맞은 <strong>{correctCount}</strong>문항 · 틀린 <strong>{wrongCount}</strong>문항
              {previewScore != null && (
                <span>{` · 환산 점수 `}<strong>{previewScore}%</strong></span>
              )}
            </>
          ) : (
            <>
              맞은 <strong>{savedRow?.correctCount ?? correctCount}</strong>/{questionCount}문항
              {savedRow?.score != null && (
                <span>{` · 환산 점수 `}<strong>{savedRow.score}%</strong></span>
              )}
            </>
          )}
        </p>

        <p className="ams-class-detail__hint-inline">
          {canManage ? '틀린 문항을 선택하면 맞은 수·점수가 자동 계산됩니다.' : '틀린 문항'}
        </p>

        <ul className="ams-wrong-question-grid">
          {Array.from({ length: questionCount }, (_, i) => {
            const no = i + 1
            const isWrong = wrongSet.has(no)
            return (
              <li key={no}>
                {canManage ? (
                  <label className={`ams-wrong-question-grid__item${isWrong ? ' ams-wrong-question-grid__item--wrong' : ''}`}>
                    <input
                      type="checkbox"
                      checked={isWrong}
                      disabled={saving}
                      onChange={() => toggleWrong(no)}
                    />
                    <span>{no}번</span>
                  </label>
                ) : (
                  <span
                    className={`ams-wrong-question-grid__item ams-wrong-question-grid__item--readonly${
                      isWrong ? ' ams-wrong-question-grid__item--wrong' : ''
                    }`}
                  >
                    {no}번{isWrong ? ' ✗' : ''}
                  </span>
                )}
              </li>
            )
          })}
        </ul>
      </div>

      <footer className="ams-homework-modal__footer">
        <button type="button" className="ams-btn ams-btn--ghost" disabled={saving} onClick={onClose}>
          {canManage ? '취소' : '닫기'}
        </button>
        {canManage && (
          <button
            type="button"
            className="ams-btn ams-btn--primary"
            disabled={saving || !dirty}
            onClick={handleSave}
          >
            {saving ? '저장 중…' : '저장'}
          </button>
        )}
      </footer>
    </ModalBackdrop>
  )
}

export async function openAnswerKeyPdf(url) {
  const response = await fetchWithAuth(`${API_BASE}${url}`)
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message || '정답지를 불러올 수 없습니다.')
  }
  const blob = await response.blob()
  const objectUrl = URL.createObjectURL(blob)
  window.open(objectUrl, '_blank', 'noopener,noreferrer')
  setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000)
}
