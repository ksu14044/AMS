# REST API 목록 (초안)

Base URL: `/api/v1` (Phase 0에서 도입). 인증: `Authorization: Bearer {jwt}`

## Auth (Phase 0)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/auth/signup/staff` | 교직원 가입 |
| POST | `/auth/signup/student` | 학생 가입 |
| POST | `/auth/login` | 로그인 (이메일·비밀번호). 다중 소속 시 `loginToken`·`academies` 반환 |
| POST | `/auth/login/select` | 다중 소속 시 학원 선택 후 토큰 발급 |
| POST | `/auth/refresh` | 토큰 갱신 |
| POST | `/admin/users/{id}/approve` | 원장 승인 — body: `role`, `subject`(선택) 확정/수정 ([DECISIONS](../DECISIONS.md) §1) |
| GET | `/admin/users/pending` | 승인 대기 교직원 목록 (원장) |

## Academy & Users (Phase 0~1)

| Method | Path | 역할 |
|--------|------|------|
| GET | `/admin/students` | 관리자 |
| GET | `/admin/teachers` | 관리자 |
| GET | `/admin/assistants` | 관리자 |

## Class (Phase 1~2)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/admin/classes` | 반 생성 |
| GET | `/classes` | 역할별 반 목록 |
| GET | `/classes/{id}` | 반 상세(7섹션) |
| PATCH | `/classes/{id}/schedule` | 수업 정보 |
| GET/POST | `/academy/notices` | 학원 공지 (행정·관리자) |
| GET/POST | `/classes/{id}/notices` | 반 공지 |
| GET/PATCH | `/classes/{id}/textbook` | 교재 (담임 수정, 조교 GET만) |
| GET/POST | `/classes/{id}/videos` | 영상 |
| GET/POST | `/classes/{id}/homeworks` | 숙제 |
| PATCH | `/classes/{id}/homeworks/{hid}/submissions/{studentId}` | 학생별 숙제 제출·점수 |
| GET/POST | `/classes/{id}/tests` | 테스트 |
| PATCH | `/classes/{id}/tests/{tid}/scores` | 학생별 점수 일괄/개별 → 보고서 트리거 |

## Enrollment (Phase 1)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/admin/classes/{id}/enrollments` | 학생 배정 |
| DELETE | `/admin/enrollments/{id}` | 배정 해제 |

## Assistant (Phase 3)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/classes/{id}/assistants` | 조교 담당 반 지정 |
| GET | `/assistant/classes` | 조교 담당 반 목록 |

## Video certification (Phase 4)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/videos/{id}/certifications` | multipart 인증사진 |

## Clinic (Phase 5)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/classes/{id}/clinic/slots` | 슬롯 생성 |
| GET | `/classes/{id}/clinic/weeks/{date}` | 주간 슬롯·예약 |
| PUT | `/classes/{id}/clinic/reservations` | 학생 예약/변경 |
| PATCH | `/clinic/reservations/{id}/result` | 결과 입력 |

## Study record (Phase 6)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/classes/{id}/study-records/me` | 학생 본인 |
| GET | `/classes/{id}/study-records/students` | 담임·관리자 (학생 목록) |
| GET | `/classes/{id}/study-records/students/{sid}` | 담임·관리자 (학생별) |

테스트 항목: `ratePercent` = `percentile_rank` 평균(높을수록 우수). `test` 응답은 `StudyRecordTestMetricResponse`(응시/마감·최근 점수 요약).

## Report (Phase 7)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/classes/{id}/reports` | 목록 (학생=본인만, 조교=403) |
| GET | `/reports/{id}` | 상세 |
| GET | `/reports/{id}/pdf` | PDF 다운로드 |
| GET | `/classes/{id}/reports/tests/{testId}/pdf-archive` | 해당 시험 보고서 PDF ZIP (담임·관리자) |
| PATCH | `/reports/{id}/comment` | 담임 코멘트 |

트리거: 보고서 탭 `POST …/reports/generate/{testId}` (수동). 구간 0건 항목은 rate/grade `null`. 종합: 숙제·클리닉 % + **이번 시험 원점수(0~100%)** ×0.3 재가중. 표시용 상위 %·percentile은 상세 필드.

## Admin · 운영 (Phase 11, 예정)

| Method | Path | 설명 | 상태 |
|--------|------|------|------|
| DELETE | `/admin/classes/{id}` | 반 아카이브(소프트 삭제)·기록 스냅샷 | 🔲 V17만 |

## Notification (Phase 8)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/notifications` | 목록 (?unreadOnly) |
| PATCH | `/notifications/{id}/read` | 읽음 |
| POST | `/devices/fcm-token` | 토큰 등록 |

## 공통 응답

```json
{
  "success": true,
  "data": { },
  "error": null
}
```

에러 시 HTTP 4xx/5xx + `error.code`, `error.message`
