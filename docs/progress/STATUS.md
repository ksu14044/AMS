# AMS 진행 현황 (라이브)

> **마지막 갱신:** 2026-06-03 (12-9 학부모 ✅)  
> Phase 경계: [PHASE-SCOPE.md](./PHASE-SCOPE.md) · 기획: [DECISIONS §10~§19](../planning/DECISIONS.md)

## 한눈에 보기

| 항목 | 상태 |
|---|---|
| **현재 Phase** | **12** v3.0 마무리 → **11** 운영 |
| **다음 Phase** | 11 운영 |
| **최종 Phase** | **11 운영** |
| **전체 진행** | Phase 0~10 ✅ / **12-1~12-9 ✅** / 11 🔲 |

## 실행 순서 (2026-05-30 확정)

```
Phase 0~10 ✅  →  10.2 (BE, 선택)  →  12 v3.0  →  11 운영 (최종)
```

---

## v3.0 기획 확정 (2026-05-30) ✅

기획 문서 반영 완료. 구현은 **Phase 12**.

| 영역 | 요약 |
|------|------|
| 수업기록 | 날짜별 1건 + 숙제·테스트·클리닉·영상 귀속 |
| 채점 | OCR 없음 · 정오표 수동 · 교직원 only |
| 테스트 | 석차 · 재시험 3회 · 마감 없음 |
| 알림 | 완료 시 소멸 · D-1 폐기 |
| 학부모 | PARENT · 자녀 연결 · 읽기 전용 |
| 보고서 | 기간·프리셋·학생 선택 생성 |

상세: [planning/01~09](../planning/) · [ROADMAP Phase 12](../planning/ROADMAP.md)

---

## Phase 8 — 알림 ✅ (v2.0, Phase 12에서 개편 예정)

| 슬라이스 | 상태 | v3.0 이관 |
|----------|------|-----------|
| 8-1~8-5 | ✅ | §16: ACTIVE/DISMISSED, D-1 **폐기** |
| 8-6 D-1 스케줄 | ✅ → **deprecated** | Phase 12-6 |
| FCM | 🔲 | Phase 11 또는 v3.0 이후 |

## Phase 2~7 요약 ✅ (v2.0 — Phase 12에서 리팩터)

| Phase | 핵심 | Phase 12 영향 |
|-------|------|---------------|
| 2 반 7섹션 | V3~V10 | → 8섹션 + `lesson_record` |
| 4 영상 | V12 | → 대상 학생만 인증 |
| 5 클리닉 | V13 | → 프리셋 · 시작시각 only |
| 6 공부기록 | — | → percentile → 점수 % |
| 7 보고서 | V14·V15 | → 기간·학생 선택 |

상세: [PHASE-SCOPE.md](./PHASE-SCOPE.md)

---

## Phase 9~10 ✅

| Phase | 상태 |
|-------|------|
| 9 UI (기능) | ✅ |
| 10 디자인 개편 + 10.1 | ✅ |

---

## Phase 10.2 — 디자인 정합 (BE) 🔲

| 슬라이스 | 상태 |
|----------|------|
| 10.2-1 `ClassResponse` 확장 | 🔲 |
| 10.2-2 학생 홈 카드 메타 | 🔲 |
| 10.2-3 반 카드 알림 dot | 🔲 |

> v3.0 착수 **전** 또는 12-1과 병행 가능.

---

## Phase 12 — v3.0 ✅

| 슬라이스 | 상태 | 비고 |
|----------|------|------|
| **12-1** 수업기록 | ✅ | 게시판 UI · 통합 등록 · 클리닉 달력 |
| **12-2** 정오표·채점 | ✅ | V23 · answer-keys · grade API · UI |
| **12-3** 석차·재시험 | ✅ | V24 · rank · retake API · UI |
| **12-4** 대상 학생·영상 | ✅ | V26 · `assignment_target` · StudentTargetPicker |
| **12-5** 조회 시작일 | ✅ | V27 · 수업기록/숙제/테스트/영상 학생 노출 필터 |
| **12-6** 알림 | ✅ | V28 · pending-count · 미완료 목록 · 홈 카드 |
| **12-7** 클리닉 프리셋 | ✅ | V29 · preset CRUD · result_json · 동적 결과 폼 |
| **12-8** 보고서 | ✅ | V31 · 기간·프리셋·학생 선택 · ZIP |
| **12-9** 학부모 | ✅ | V32 · PARENT · parent-links · `/parent/*` |

### 12-1 완료 ✅

- `V21__lesson_record.sql` · `GET/POST/PATCH /lesson-records`
- 수업기록 게시판 UI · 통합 등록 (숙제·테스트·영상·클리닉)
- 숙제·테스트 탭 확인용 · wide 레이아웃
- `V22` 클리닉 토·일 허용

### 12-2 완료 ✅

- `V23__homework_test_v3.sql` — `question_count`, `homework_answer_key`, `due_at` 제거
- `GET/PUT .../homeworks/{id}/answer-keys` · `PATCH .../submissions/{studentId}/grade`
- `HomeworkScoreCalculator` — `ceil(100÷문항수×맞은 수, 소수 1자리)`
- 숙제 확인 탭 — 정답지·문항별 답안·자동 점수 (`canEditContent`)

### 12-3 완료 ✅

- `V24__test_rank_retake.sql` — `question_count`, `retake_threshold_count`, `parent_test_id`, `retake_attempt_no`, `test_score.rank`
- `TestRankCalculator` — 동점 공동·건너뛰기 (100·100·99 → 1·1·3)
- `TestRetakeEvaluator` · `POST .../tests/{id}/retakes` — 최대 3회 · 본·재시험 석차 독립
- `TestScoreCalculator` · `StudyRecordService` — root별 최종 시험 `raw_score` 산술 평균 (percentile deprecated)
- 테스트 확인 탭 — 석차 표시 · 재시험 생성 모달 (`canEditContent`)
- 수업기록 통합 등록 — 테스트 문항 수·합격 기준(맞은 문항 수) 입력
- `V25__test_answer_key_grading.sql` — `test_answer_key`, `test_score.answers/correct_count`
- `GET/PUT .../tests/{id}/answer-keys` · `PATCH .../scores/{studentId}/grade` · `PATCH .../complete`
- 테스트 확인 UI — 정답지·학생별 답안·자동 점수 · 시험 완료 시 석차 계산

### 12-4 완료 ✅

- `V26__assignment_target.sql` — `assignment_target(entity_type, entity_id, student_id)`
- `AssignmentTargetService` — 숙제·테스트·클리닉 기본 반 전원 · 영상 `assignment_target` = **인증 대상** (시청은 전원)
- 목록·제출·예약 — 대상 학생만 · 영상은 **전원 시청** · **지정 학생만 인증**
- `StudentTargetPicker` — 숙제·테스트·영상·클리닉·수업기록 통합 등록 폼 연동
- 영상 규칙 정정 — **전원 시청** · `assignment_target` = **인증 제출 대상만**
- 영상 탭 썸네일 컴팩트 레이아웃 (CSS)

### 12-6 완료 ✅

- `V28__notification_status.sql` — `notification.status` ACTIVE/DISMISSED
- `PendingTaskService` · `GET /me/pending-tasks` · `GET /notifications/badge-count`
- 과제형 알림: 숙제·테스트=점수, 영상=인증, 클리닉=결과 입력 시 DISMISSED
- 정보형 알림: 읽음 시 DISMISSED · 뱃지는 미완료 건수(읽음 무관)
- 숙제 D-1 스케줄 제거 · 테스트 `test_at` 전날 알림 유지
- FE: 뱃지 · 알림 미완료 카드 · `/notifications/pending` · 학생 홈 미완료 카드

### 12-7 완료 ✅

- `V29__clinic_result_preset.sql` — `clinic_result_preset`, `clinic_slot.clinic_result_preset_id`, `clinic_reservation.result_json`
- 프리셋 CRUD · 슬롯·수업기록 등록 시 **프리셋 선택** · 정원 기본 **10**
- 결과 저장 API `{ result: { ... } }` · 프리셋 스키마 검증
- 기존 반·슬롯 **「기본」** 프리셋(참석+메모) 자동 마이그레이션 · 신규 반 생성 시 기본 프리셋
- FE: 프리셋 관리 UI · `ClinicSlotResultTable` 동적 입력 · 클리닉·수업기록 연동
- 후속: 결과 테이블 UX (열 정렬 · 2줄 textarea · 긴 텍스트 펼치기)
- V29 H2 호환 SQL (CI) · 로컬 checksum mismatch 시 `flyway:repair` 1회 (`application-local.yaml.example` 참고)

### 12-8 완료 ✅

- `V31__report_period_preset.sql` — `report_period_preset` · `diligence_report.period_label/preset_id/test_rank` · `test_id` nullable
- `ReportPeriodPresetService` · `GET/POST/PATCH/DELETE .../report-period-presets`
- `POST .../reports/generate` — `{ periodStart, periodEnd, studentIds[], presetId? }` · 기간 내 숙제·클리닉·영상·**완료 시험 전체** 집계
- `StudyRecordPeriodCalculator.computeTestMetrics` — 루트별 최신 재시험 % 평균 · 석차=기간 내 최근 시험
- `POST .../reports/archive` — 기간별 PDF ZIP · 목록 기간 그룹
- FE: `ClassReportsSection` — 프리셋 CRUD · 생성(학생 일괄) · 기간 ZIP · 상세 모달(석차)
- v2 `POST .../generate/{testId}` — deprecated 유지
- Flyway: H2 `AFTER` 제거 · FK 선삭제 · `scripts/flyway-repair-local.ps1`

### 12-9 완료 ✅

- `V32__parent_student_link.sql` · `UserRole.PARENT`
- `POST /auth/signup/parent` · 학부모 가입 링크(관리자)
- `POST/DELETE /parent-links` · 학생부 「연결」UI
- `GET /parent/children` · 자녀별 미완료·공부기록·보고서(PDF) **읽기 전용**

---

### 수업기록 귀속 항목 CRUD ✅ (12-1 보완)

- `POST/PATCH/DELETE .../lesson-records/{id}/linked-items` — 숙제·테스트·영상·클리닉 **추가·수정·삭제**
- `LessonRecordLinkedItemResponse` — `canDelete` · `canEdit` (재시험 등 잠금 구분)
- 상세 패널 UI — **+ 항목 추가** · 항목별 **수정** / **삭제** · `StudentTargetPicker`
- 수업일 변경은 **미구현** (의도적 — 1일 1건 유지)
- 커밋: `b90398b` 수업 기록 귀속 항목 수정 추가

---

## Phase 11 — 운영 🔲 **(최종)**

> **Phase 12 완료 후** 진행. E2E·보안·캐싱·감사·아카이브.

| 슬라이스 | 상태 |
|----------|------|
| 11-2 V17·아카이브 API | 🟡 SQL만 |
| 11-1 E2E·Playwright | 🔲 |
| 11-3 rate limit·MIME | 🔲 |
| 11-4 React Query | 🔲 |
| 11-5 Testcontainers | 🔲 |

---

## 오늘 할 일 (2026-06-03)

1. ~~**12-8** 보고서 v3.0~~ ✅
2. ~~**12-9** 학부모~~ ✅

## 다음 할 일

1. **Phase 11** — 운영(E2E·감사·rate limit 등)

---

## Phase 진행표

| Phase | 이름 | 상태 | 비고 |
|-------|------|------|------|
| 0~7 | 기반~보고서 | ✅ | v2.0 |
| 8 | 알림 | ✅ | 12-6에서 개편 |
| 9 | UI | ✅ | |
| 10 | 디자인 | ✅ | 10.1 포함 |
| 10.2 | BE 정합 | 🔲 | 선택 |
| **12** | **v3.0** | ✅ | **12-1~12-9** |
| **11** | **운영** | 🔲 | **최종** |

---

## 최근 일지

| 날짜 | 요약 |
|------|------|
| [2026-06-03](./daily/2026-06-03.md) | 12-8 보고서 v3.0 ✅ · Flyway/CI |
| [2026-06-01](./daily/2026-06-01.md) | 12-5~12-7 · 알림·클리닉 프리셋 · V29/Flyway·CI 정리 |
| [2026-05-30](./daily/2026-05-30.md) | Phase 12-1 수업기록 API·UI |
| [2026-05-29](./daily/2026-05-29.md) | 조교 클리닉·학생부 · 인사·로그아웃 |
| [2026-05-28](./daily/2026-05-28.md) | 보고서 클리닉 구간·PDF·ZIP |
| [2026-05-26](./daily/2026-05-26.md) | Phase 10.1 목업 정합 |

---

## 빠른 링크

- [PHASE-SCOPE](./PHASE-SCOPE.md)
- [ROADMAP](../planning/ROADMAP.md)
- [DECISIONS §10~§19](../planning/DECISIONS.md)
