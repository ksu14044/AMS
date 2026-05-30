# 07. 영상 수업 및 인증

## 등록 (선생님/조교)

- **수업기록**에 귀속 ([§10](./DECISIONS.md#10-수업기록-lesson-record))
- YouTube URL → oEmbed 썸네일·제목
- **대상 학생** ([§14](./DECISIONS.md#14-대상-학생-지정)): 기본 **전원 미선택**

## 수강 (학생)

- **대상 학생만** [인증사진 제출]
- **완료(알림):** 제출 시 ([§16](./DECISIONS.md#16-알림-완료-기준))
- 비대상: 조회만

## API

| Method | Path |
|--------|------|
| POST | `/classes/{id}/lesson-records/{lrId}/videos` |
| PUT | `/videos/{id}/targets` |
| POST | `/videos/{id}/certifications` |

JPEG/PNG, 최대 10MB.

Phase: Phase 4 · **Phase 12**
