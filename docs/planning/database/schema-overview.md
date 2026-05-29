# DB 스키마 개요

MySQL 8.0, 데이터베이스 `ams`. 모든 비즈니스 테이블에 `academy_id` 포함.

## Phase 0 — 인증·테넌트

```sql
academy (
  academy_id PK,
  name, code UNIQUE,  -- 학원 코드
  created_at
)

user (
  user_id PK,
  academy_id FK,
  email UNIQUE per academy,
  password_hash,
  name,
  role ENUM,           -- ACADEMY_ADMIN | TEACHER_KO/EN/MATH | STAFF_OFFICE | ASSISTANT_KO/EN/MATH | STUDENT
  subject ENUM NULL,   -- KO, EN, MATH (담임·조교)
  status ENUM,         -- PENDING, ACTIVE, SUSPENDED
  created_at
)
```

## Phase 1 — 반·수강

```sql
class (
  class_id PK,
  academy_id FK,
  subject ENUM,
  name,                -- e.g. 국어 남고1반
  homeroom_teacher_id FK user,
  classroom,           -- optional
  created_at
)

class_enrollment (
  enrollment_id PK,
  class_id FK,
  student_id FK user,
  assigned_at,
  assigned_by FK user,
  UNIQUE(class_id, student_id)
)

assistant_class_assignment (
  assignment_id PK,
  assistant_id FK user,
  class_id FK,
  assigned_by FK user,
  UNIQUE(assistant_id, class_id)
)
```

## Phase 2 — 반 7섹션

```sql
class_schedule (class_id, day_of_week, start_time, end_time, room)

academy_notice (notice_id, academy_id, title, body, ...)  -- 행정·관리자 학원 공지

class_notice (notice_id, class_id, title, body, attachment_url, published_at, scheduled_at, author_id)

textbook (class_id PK or id, title, publisher, progress_note, updated_at)

video_lesson (video_id, class_id, youtube_url, title, thumbnail_url, published_at, ...)

homework (
  homework_id, class_id, title, due_at, status SCHEDULED|COMPLETED
)

homework_submission (
  submission_id, homework_id, student_id,
  submitted BOOLEAN, submitted_at, score, grade, memo
)

test (
  test_id, class_id, title, test_at, status SCHEDULED|COMPLETED,
  class_average, -- 시험 종료 후 집계
  completed_at
)

test_score (
  score_id, test_id, student_id,
  raw_score, grade,
  class_avg,           -- 반 평균 (보고서 표시)
  upper_rank_pct,      -- 상위 % (보고서·UI)
  percentile_rank      -- 0~100, 게이지 30%용 ([DECISIONS](../DECISIONS.md) §9)
)

clinic_slot (
  slot_id, class_id, week_start_date,
  day_of_week ENUM MON..FRI,  -- 원본 월~금만
  start_time, assistant_id, max_capacity
)
```

## Phase 5 — 클리닉 예약

```sql
clinic_week (class_id, week_start, status OPEN|LOCKED)

clinic_reservation (
  reservation_id, slot_id, student_id,
  status EMPTY|RESERVED|CONFIRMED,
  result_attended, result_memo, ...
)
```

## Phase 4 — 영상 인증

```sql
video_certification (
  certification_id, video_id, student_id,
  image_url, submitted_at
)
```

## Phase 6 — 공부기록 (구현)

- 별도 `study_activity_log` **미사용** — 숙제·클리닉·테스트·영상 테이블 실시간 집계
- API: `GET /classes/{id}/study-records/...`

## Phase 7 — 성실도 보고서 (V14, V15)

```sql
diligence_report (
  report_id, class_id, student_id, test_id,
  period_start, period_end,
  homework_submitted, homework_total, homework_rate NULL, homework_grade NULL,
  clinic_attended, clinic_total, clinic_rate NULL, clinic_grade NULL,
  test_raw_score, test_class_avg, test_upper_rank_pct, test_percentile_rank, test_grade,
  video_certified, video_total, video_rate NULL, video_grade NULL,
  total_score, overall_grade, teacher_comment, pdf_path, created_at
)
```

UK: `(test_id, student_id)`. V15: 집계 대상 0건 시 rate/grade NULL.

## Phase 11 — 감사·반 아카이브 (V17, API 미구현)

```sql
audit_log (
  audit_id, academy_id, actor_user_id,
  action, entity_type, entity_id, detail_json, created_at
)

class (
  ... 기존 컬럼 ...,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | ARCHIVED 등
  archived_at TIMESTAMP NULL
)

class_record_archive (
  archive_id, academy_id, class_id, class_name, subject,
  archived_by, archived_at, snapshot_json
)
```

마이그레이션: `V17__audit_log_class_archive.sql`. 앱 코드·REST는 [STATUS.md](../../progress/STATUS.md) Phase 11 참고.

## Phase 8 — 알림

```sql
notification (
  notification_id, academy_id, user_id,
  type, title, body, reference_type, reference_id,
  read_at, created_at
)

device_token (user_id, fcm_token, platform)
```

## 인덱스 권장

- `(academy_id, class_id)` on 모든 class 하위 테이블
- `(student_id, class_id)` on enrollment, reservation
- `(class_id, week_start)` on clinic_slot

## 마이그레이션 파일 명명

```
ams_backend/src/main/resources/db/migration/
  V1__academy_user.sql
  V2__class_enrollment.sql
  V3__class_notice.sql
  ...
  V17__audit_log_class_archive.sql
```
