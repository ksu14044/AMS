import { useCallback, useEffect, useState } from 'react'
import {
  createClassVideo,
  deleteClassVideo,
  fetchClassVideos,
  updateClassVideo,
  toInstant,
} from '../../api/classesApi'
import { fetchMyVideoCertification, mediaUrl, uploadVideoCertification } from '../../api/videosApi'
import { useAuth } from '../../auth/AuthContext'
import StudentTargetPicker from '../../components/StudentTargetPicker'
import {
  buildTargetStudentIdsPayload,
  createInitialTarget,
  formatVideoCertTargetSummary,
  requiresVideoCertification,
} from '../../utils/assignmentTargets'
import { youtubeThumbnailUrl, youtubeWatchUrl } from '../../utils/youtube'

const EMPTY_FORM = { youtubeUrl: '', title: '', description: '', publishedDate: '' }

export default function ClassVideoSection({ classId, canManage, isStudent, onError }) {
  const { user } = useAuth()
  const studentId = isStudent ? user?.userId : null
  const [videos, setVideos] = useState([])
  const [certs, setCerts] = useState({})
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [createTarget, setCreateTarget] = useState(() => createInitialTarget(false))
  const [editingId, setEditingId] = useState(null)
  const [editForm, setEditForm] = useState(EMPTY_FORM)

  const loadCerts = useCallback(
    async (list) => {
      if (!isStudent || !studentId) return
      const map = {}
      const certVideos = list.filter((v) => requiresVideoCertification(v, studentId))
      await Promise.all(
        certVideos.map(async (v) => {
          try {
            map[v.videoId] = await fetchMyVideoCertification(v.videoId)
          } catch {
            map[v.videoId] = null
          }
        }),
      )
      setCerts(map)
    },
    [isStudent, studentId],
  )

  const load = useCallback(async () => {
    setLoading(true)
    onError('')
    try {
      const list = await fetchClassVideos(classId)
      setVideos(list)
      await loadCerts(list)
    } catch (err) {
      onError(err.message)
    } finally {
      setLoading(false)
    }
  }, [classId, loadCerts, onError])

  useEffect(() => {
    load()
  }, [load])

  async function handleCreate(e) {
    e.preventDefault()
    if (!form.youtubeUrl.trim() || !form.title.trim()) return
    setSubmitting(true)
    onError('')
    try {
      await createClassVideo(classId, {
        youtubeUrl: form.youtubeUrl.trim(),
        title: form.title.trim(),
        description: form.description.trim() || null,
        publishedAt: form.publishedDate ? toInstant(form.publishedDate, '12:00') : null,
        targetStudentIds: buildTargetStudentIdsPayload(createTarget, false),
      })
      setForm(EMPTY_FORM)
      setCreateTarget(createInitialTarget(false))
      await load()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  function startEdit(video) {
    setEditingId(video.videoId)
    setEditForm({
      youtubeUrl: video.youtubeUrl,
      title: video.title,
      description: video.description ?? '',
      publishedDate: '',
    })
  }

  function cancelEdit() {
    setEditingId(null)
    setEditForm(EMPTY_FORM)
  }

  async function handleUpdate(e) {
    e.preventDefault()
    if (!editingId || !editForm.title.trim() || !editForm.youtubeUrl.trim()) return
    setSubmitting(true)
    onError('')
    try {
      await updateClassVideo(classId, editingId, {
        youtubeUrl: editForm.youtubeUrl.trim(),
        title: editForm.title.trim(),
        description: editForm.description.trim() || null,
      })
      cancelEdit()
      await load()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(videoId) {
    if (!window.confirm('이 영상을 삭제할까요?')) return
    setSubmitting(true)
    onError('')
    try {
      await deleteClassVideo(classId, videoId)
      if (editingId === videoId) cancelEdit()
      await load()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCertUpload(videoId, file) {
    if (!file) return
    setSubmitting(true)
    onError('')
    try {
      const cert = await uploadVideoCertification(videoId, file)
      setCerts((prev) => ({ ...prev, [videoId]: cert }))
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return <p className="ams-class-detail__empty">불러오는 중…</p>
  }

  return (
    <section className="ams-class-detail__section">
      <h3 className="ams-class-detail__heading">영상 수업</h3>
      <p className="ams-class-detail__hint-inline">
        모든 수강생이 영상을 시청할 수 있습니다. 인증 사진은 지정한 학생만 제출합니다.
      </p>

      {canManage && (
        <form className="ams-video-form" onSubmit={handleCreate}>
          <label className="ams-video-form__full">
            유튜브 URL
            <input
              type="url"
              value={form.youtubeUrl}
              onChange={(e) => setForm({ ...form, youtubeUrl: e.target.value })}
              placeholder="https://www.youtube.com/watch?v=..."
              required
            />
          </label>
          <label>
            제목
            <input
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              maxLength={200}
              required
            />
          </label>
          <label>
            게시일 (선택)
            <input
              type="date"
              value={form.publishedDate}
              onChange={(e) => setForm({ ...form, publishedDate: e.target.value })}
            />
          </label>
          <label className="ams-video-form__full">
            설명 (선택)
            <textarea
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              rows={2}
            />
          </label>
          <StudentTargetPicker
            className="ams-video-form__full"
            classId={classId}
            allByDefault={false}
            label="인증 대상 학생"
            value={createTarget}
            onChange={setCreateTarget}
            disabled={submitting}
          />
          <button type="submit" className="ams-btn ams-btn--primary" disabled={submitting}>
            영상 등록
          </button>
        </form>
      )}

      {videos.length === 0 ? (
        <p className="ams-class-detail__empty">등록된 영상이 없습니다.</p>
      ) : (
        <ul className="ams-video-list">
          {videos.map((v) => {
            const thumb =
              v.thumbnailUrl || (v.thumbnailAvailable === false ? null : youtubeThumbnailUrl(v.youtubeUrl))
            const watch = youtubeWatchUrl(v.youtubeUrl)
            const isEditing = editingId === v.videoId
            const cert = certs[v.videoId]
            const certRequired = requiresVideoCertification(v, studentId)

            return (
              <li key={v.videoId} className="ams-video-list__item">
                {isEditing ? (
                  <form className="ams-video-form ams-video-form--inline" onSubmit={handleUpdate}>
                    <label className="ams-video-form__full">
                      유튜브 URL
                      <input
                        type="url"
                        value={editForm.youtubeUrl}
                        onChange={(e) => setEditForm({ ...editForm, youtubeUrl: e.target.value })}
                        required
                      />
                    </label>
                    <label className="ams-video-form__full">
                      제목
                      <input
                        value={editForm.title}
                        onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
                        maxLength={200}
                        required
                      />
                    </label>
                    <label className="ams-video-form__full">
                      설명
                      <textarea
                        value={editForm.description}
                        onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                        rows={2}
                      />
                    </label>
                    <div className="ams-video-list__actions">
                      <button type="submit" className="ams-btn ams-btn--primary" disabled={submitting}>
                        저장
                      </button>
                      <button type="button" className="ams-btn ams-btn--ghost" onClick={cancelEdit}>
                        취소
                      </button>
                    </div>
                  </form>
                ) : (
                  <>
                    <a
                      href={watch}
                      target="_blank"
                      rel="noopener noreferrer"
                      className={
                        thumb
                          ? 'ams-video-list__player'
                          : 'ams-video-list__player ams-video-list__player--placeholder'
                      }
                      aria-label={`${v.title} 유튜브에서 보기`}
                    >
                      {thumb && (
                        <img src={thumb} alt="" className="ams-video-list__thumb" />
                      )}
                      <span className="ams-video-list__play" aria-hidden>
                        <svg
                          viewBox="0 0 24 24"
                          fill="currentColor"
                          aria-hidden
                        >
                          <path d="M8 5.5v13l11-6.5z" />
                        </svg>
                      </span>
                      {!thumb && v.thumbnailAvailable === false && (
                        <span className="ams-video-list__placeholder-text">
                          비공개·미리보기 없음
                        </span>
                      )}
                    </a>
                    <div className="ams-video-list__body">
                      <h4 className="ams-video-list__title">{v.title}</h4>
                      <p className="ams-video-list__meta">
                        <time>{new Date(v.publishedAt).toLocaleDateString('ko-KR')}</time>
                        {canManage && v.targets && (
                          <span> · {formatVideoCertTargetSummary(v.targets)}</span>
                        )}
                        {isStudent && certRequired && (
                          <span
                            className={
                              cert
                                ? 'ams-video-list__cert-badge ams-video-list__cert-badge--done'
                                : 'ams-video-list__cert-badge'
                            }
                          >
                            {cert ? '· 인증사진 제출' : '· 인증 대기'}
                          </span>
                        )}
                      </p>
                      {v.description && (
                        <p className="ams-video-list__desc">{v.description}</p>
                      )}
                    </div>

                    {isStudent && certRequired && (
                      <div className="ams-video-cert">
                        {cert ? (
                          <p className="ams-video-cert__done">
                            인증 완료 ({new Date(cert.submittedAt).toLocaleString('ko-KR')})
                            {cert.imageUrl && (
                              <a
                                href={mediaUrl(cert.imageUrl)}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="ams-video-list__link"
                              >
                                제출 사진 보기
                              </a>
                            )}
                          </p>
                        ) : (
                          <label className="ams-video-cert__upload">
                            <span className="ams-btn ams-btn--ghost">인증사진 제출</span>
                            <input
                              type="file"
                              accept="image/jpeg,image/png"
                              className="ams-video-cert__input"
                              disabled={submitting}
                              onChange={(e) => {
                                const f = e.target.files?.[0]
                                if (f) handleCertUpload(v.videoId, f)
                                e.target.value = ''
                              }}
                            />
                          </label>
                        )}
                      </div>
                    )}

                    {canManage && (
                      <div className="ams-video-list__actions">
                        <button
                          type="button"
                          className="ams-btn ams-btn--ghost"
                          disabled={submitting}
                          onClick={() => startEdit(v)}
                        >
                          수정
                        </button>
                        <button
                          type="button"
                          className="ams-btn ams-btn--ghost"
                          disabled={submitting}
                          onClick={() => handleDelete(v.videoId)}
                        >
                          삭제
                        </button>
                      </div>
                    )}
                  </>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
