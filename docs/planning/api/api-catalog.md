# REST API 목록 (초안)

Base URL: `/api/v1`. Bearer JWT.

## Auth (v3.0: parent signup)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/auth/signup/parent` | 학부모 가입 |
| POST | `/auth/signup/student` | 학생 가입 |
| POST | `/auth/signup/staff` | 교직원 가입 |

(기존 login, refresh, approve 유지)

## Parent (Phase 12)

| Method | Path |
|--------|------|
| POST | `/admin/parent-links` |
| GET | `/parent/children` |
| GET | `/parent/children/{sid}/pending` |
| GET | `/parent/children/{sid}/classes/{cid}/study-records` |

## Enrollment (v3.0)

| Method | Path |
|--------|------|
| POST | `/admin/classes/{id}/enrollments` | body: `accessibleFrom` |
| PATCH | `/admin/enrollments/{id}` |

## Lesson record (Phase 12)

| Method | Path |
|--------|------|
| GET/POST | `/classes/{id}/lesson-records` |
| GET/PATCH | `/classes/{id}/lesson-records/{lrId}` |

## Homework · Test (v3.0)

| Method | Path |
|--------|------|
| POST | `.../lesson-records/{lrId}/homeworks` |
| PUT | `/homeworks/{hid}/answer-key` |
| PUT | `/homeworks/{hid}/submissions/{studentId}` |
| POST | `.../lesson-records/{lrId}/tests` |
| PUT | `/tests/{tid}/scores/{studentId}` |
| POST | `/tests/{tid}/retakes` |

점수: `ceil(100/q×correct, 1)` ([§11](../DECISIONS.md#11-정오표채점-수동)).

## Clinic preset

| Method | Path |
|--------|------|
| GET/POST | `/classes/{id}/clinic-presets` |
| PATCH | `/clinic/reservations/{id}/result` |

## Video (v3.0)

| Method | Path |
|--------|------|
| POST | `.../lesson-records/{lrId}/videos` |
| PUT | `/videos/{id}/targets` |

## Study record

| Method | Path |
|--------|------|
| GET | `/classes/{id}/study-records/me?from=&to=` |

테스트 `ratePercent` = **`raw_score` 평균** (v3.0).

## Report (v3.0)

| Method | Path |
|--------|------|
| GET/POST | `/classes/{id}/report-period-presets` |
| POST | `/classes/{id}/reports/generate` |

body: `{ periodStart, periodEnd, studentIds[] }`

~~POST …/generate/{testId}~~ — 대체.

## Notification (v3.0)

| Method | Path |
|--------|------|
| GET | `/notifications?status=ACTIVE` |
| PATCH | `/notifications/{id}/read` |

완료 시 내부 `DISMISSED`.

## Class · 기존 (Phase 1~2)

공지·일정·교재·클리닉 예약·영상 인증 등 — Phase 12에서 lesson-record 경로로 점진 이전.

[04-class-data-model.md](../04-class-data-model.md) · [DECISIONS.md](../DECISIONS.md) §10~§19.
