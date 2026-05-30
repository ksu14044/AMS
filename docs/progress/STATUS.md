# AMS 진행 현황 (라이브)

> **마지막 갱신:** 2026-05-30 (Phase 12-4 완료)  
> Phase 경계: [PHASE-SCOPE.md](./PHASE-SCOPE.md) · 기획: [DECISIONS §10~§19](../planning/DECISIONS.md)

## 한눈에 보기

| 항목 | 상태 |
|---|---|
| **현재 Phase** | **12-5** 접근 시점 (`accessible_from`) 🔲 |
| **다음 Phase** | 12-6~12-9 |
| **최종 Phase** | **11 운영** |
| **전체 진행** | Phase 0~10 ✅ / **12-1~12-4 ✅** / 12-5~9·11 🔲 |

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

## Phase 12 — v3.0 🟡

| 슬라이스 | 상태 | 비고 |
|----------|------|------|
| **12-1** 수업기록 | ✅ | 게시판 UI · 통합 등록 · 클리닉 달력 |
| **12-2** 정오표·채점 | ✅ | V23 · answer-keys · grade API · UI |
| **12-3** 석차·재시험 | ✅ | V24 · rank · retake API · UI |
| **12-4** 대상 학생·영상 | ✅ | V26 · `assignment_target` · StudentTargetPicker |
| 12-5~12-9 | 🔲 | |

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

## 다음 할 일

1. **12-5** — `accessible_from` (접근 시점)
2. ~~Phase 11~~ — 12 완료 후

---

## Phase 진행표

| Phase | 이름 | 상태 | 비고 |
|-------|------|------|------|
| 0~7 | 기반~보고서 | ✅ | v2.0 |
| 8 | 알림 | ✅ | 12-6에서 개편 |
| 9 | UI | ✅ | |
| 10 | 디자인 | ✅ | 10.1 포함 |
| 10.2 | BE 정합 | 🔲 | 선택 |
| **12** | **v3.0** | 🟡 | **12-1~12-4 ✅** |
| **11** | **운영** | 🔲 | **최종** |

---

## 최근 일지

| 날짜 | 요약 |
|------|------|
| [2026-05-30](./daily/2026-05-30.md) | Phase 12-1 수업기록 API·UI |
| [2026-05-29](./daily/2026-05-29.md) | 조교 클리닉·학생부 · 인사·로그아웃 |
| [2026-05-28](./daily/2026-05-28.md) | 보고서 클리닉 구간·PDF·ZIP |
| [2026-05-26](./daily/2026-05-26.md) | Phase 10.1 목업 정합 |

---

## 빠른 링크

- [PHASE-SCOPE](./PHASE-SCOPE.md)
- [ROADMAP](../planning/ROADMAP.md)
- [DECISIONS §10~§19](../planning/DECISIONS.md)
