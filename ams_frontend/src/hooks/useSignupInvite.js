import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { fetchSignupInvite } from '../api/authApi'

export function useSignupInvite(expectedKind) {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const [invite, setInvite] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token) {
      setError('유효한 가입 링크가 필요합니다. 학원에서 받은 링크로 접속해 주세요.')
      setLoading(false)
      return
    }
    let cancelled = false
    setLoading(true)
    setError('')
    fetchSignupInvite(token)
      .then((data) => {
        if (cancelled) return
        if (data.kind !== expectedKind) {
          setError('이 가입 링크는 이 화면에서 사용할 수 없습니다.')
          return
        }
        setInvite(data)
      })
      .catch((err) => {
        if (!cancelled) setError(err.message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [token, expectedKind])

  return { token, invite, loading, error }
}
