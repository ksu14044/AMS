/** Phase 2-5: URL만 저장. 썸네일 자동(oEmbed)은 Phase 4. */

export function extractYoutubeVideoId(url) {
  if (!url) return null
  try {
    const u = new URL(url.trim())
    const host = u.hostname.replace(/^www\./, '')
    if (host === 'youtu.be') {
      const id = u.pathname.slice(1).split('/')[0]
      return id || null
    }
    if (host === 'youtube.com' || host === 'm.youtube.com') {
      if (u.pathname.startsWith('/embed/')) {
        return u.pathname.split('/')[2] || null
      }
      if (u.pathname.startsWith('/shorts/')) {
        return u.pathname.split('/')[2] || null
      }
      return u.searchParams.get('v')
    }
  } catch {
    return null
  }
  return null
}

export function youtubeThumbnailUrl(url) {
  const id = extractYoutubeVideoId(url)
  return id ? `https://img.youtube.com/vi/${id}/hqdefault.jpg` : null
}

export function youtubeWatchUrl(url) {
  const id = extractYoutubeVideoId(url)
  return id ? `https://www.youtube.com/watch?v=${id}` : url
}
