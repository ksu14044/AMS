import { Link } from 'react-router-dom'

export default function SignupInviteGate({ loading, error, invite, children }) {
  if (loading) {
    return (
      <div className="ams-card ams-auth__card">
        <p className="ams-card__desc">가입 링크 확인 중…</p>
      </div>
    )
  }

  if (error || !invite) {
    return (
      <div className="ams-card ams-auth__card">
        <h1 className="ams-card__title">가입 링크 필요</h1>
        <p className="ams-form__error">{error || '가입 링크를 확인할 수 없습니다.'}</p>
        <p className="ams-card__desc">
          회원가입은 학원(또는 운영팀)에서 발송한 전용 링크로만 가능합니다.
        </p>
        <div className="ams-card__footer">
          <Link to="/login">로그인으로 돌아가기</Link>
        </div>
      </div>
    )
  }

  return children(invite)
}
