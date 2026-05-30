# DB 스키마 개요

MySQL 8.0, `ams`. 모든 비즈니스 테이블 `academy_id`.

## Phase 0 — user (v3.0: PARENT)

```sql
user (
  role ENUM,  -- ... | STUDENT | PARENT
  ...
)
```

## Phase 1 — enrollment · parent (v3.0)

```sql
class_enrollment (
  accessible_from DATE NULL,  -- v3.0
  ...
)

parent_student_link (
  link_id PK,
  parent_id FK, student_id FK,
  linked_by FK, linked_at,
  UNIQUE(parent_id, student_id)
)
```

## v3.0 — lesson_record

```sql
lesson_record (
  record_id PK, class_id FK,
  lesson_date DATE, summary TEXT,
  author_id FK, UNIQUE(class_id, lesson_date)
)

report_period_preset (
  preset_id, class_id FK NULL,
  name, period_start, period_end
)

clinic_result_preset (
  preset_id, class_id FK,
  name, field_schema JSON
)
```

## v3.0 — homework · test

```sql
homework (
  lesson_record_id FK,
  question_count INT,
  status OPEN|COMPLETED
  -- due_at removed
)

homework_answer_key (homework_id, question_no, correct_answer)
homework_submission (answers JSON, correct_count, raw_score, completed_at)

test_exam (
  lesson_record_id FK,
  question_count, retake_threshold_count,
  parent_test_id FK NULL,
  retake_attempt_no INT DEFAULT 0
)

test_score (answers JSON, raw_score, rank INT, class_avg)
-- upper_rank_pct, percentile_rank deprecated

assignment_target (entity_type, entity_id, student_id)
```

## Clinic · Video (v3.0)

```sql
clinic_slot (
  lesson_record_id FK, start_time,
  max_capacity DEFAULT 10,
  clinic_result_preset_id FK
)
clinic_reservation (result_json JSON, result_saved_at)

video_lesson (lesson_record_id FK)
-- targets via assignment_target, default empty
```

## Report (v3.0)

```sql
diligence_report (
  period_start, period_end,
  period_preset_id FK NULL,
  test_rank INT NULL,
  -- test_id optional
)
```

## Notification (v3.0)

```sql
notification (
  status ACTIVE|DISMISSED,
  read_at, dismissed_at
)
```

## 마이그레이션 (예정)

```
V21__lesson_record.sql
V22__homework_test_v3.sql
V23__clinic_preset.sql
V24__parent_role.sql
V25__notification_dismissed.sql
V26__report_period_preset.sql
```

[ROADMAP.md](../ROADMAP.md) Phase 12.

## Phase 0~11 기존 테이블

`academy`, `class`, `class_notice`, `class_schedule`, `textbook`, `clinic_week`, `video_certification`, `diligence_report`(V14), `audit_log`(V17) 등 — Phase 12에서 v3.0 컬럼·FK 추가.
