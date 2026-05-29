# Phase 완료 범위·진행 슬라이스

> **목적:** “Phase N 완료”의 의미를 한곳에 모아, ROADMAP 체크박스와 실제 코드 차이를 줄인다.  
> **갱신:** 2026-05-26 (Phase 10=디자인, Phase 11=운영 · 보고서 생성 분리)

## Phase 10 — 프론트엔드 디자인 전면 개편 (2026-05-26)

| 슬라이스 | 항목 | 상태 | 비고 |
|----------|------|------|------|
| 10-1 | 토큰·글로벌·레이아웃 | ✅ | `design-tokens`·`components.css`·`AppLayout` 학생 헤더·`BottomNav` outline |
| 10-2 | 인증·온보딩 | ✅ | `auth.css` 재작성·focus ring·signup links pill·PendingApproval 목업화 |
| 10-3 | 학생 홈·탭·클리닉·기록·MY | ✅ | 과목 큰 카드·반 카드·MY 정합 (`class-list.css` 재작성) |
| 10-4 | 반 상세 7섹션 | ✅ | 헤더(← back) + 탭 pill + 클리닉 슬롯 pill·강조 + 보고서 검정 카드 |
| 10-5 | 교직원·행정·알림 | ✅ | `admin.css` 토큰화·중복 제거, `AcademyNotices` → `class-detail.css` 이관, `NotificationBell` SVG+badge, Notifications 카드·필터 정합 |
| 10-6 | 관리자 반·공지 | ✅ | `AdminClassesSection` 반 카드 row 구조, 폼·배정 해제 ghost-sm 정합 |
| 10-7 | 공통 컴포넌트 | ✅ | 미사용 `App.css`/`student.css` 삭제, study-record 진행바 토큰 통일, NotificationBell `.ams-icon-btn` 재사용 |
| 10-8 | 반응형·a11y | ✅ | 클래스 상세 탭 가로 스크롤(모바일)/wrap(≥768px), `.ams-app__container { overflow-x: hidden }`, focus-visible/터치 영역 점검 |

## Phase 10.1 — 목업 정합 보강 (2026-05-26, frontend only)

> [10-frontend-design.md §10.1](../planning/10-frontend-design.md) — 두 PNG 목업 100% 정합 + UX 결정 3건 ((a) ⋮ placeholder · (A) 기록 탭 반 리스트 유지 · (Y) 클리닉 명시적 저장).

| 슬라이스 | 항목 | 상태 | 비고 |
|----------|------|------|------|
| 10.1-1 | 반 상세 헤더 | ✅ | 3-col grid (← / 가운데 제목+부제 / ⋮ disabled placeholder), title ellipsis |
| 10.1-2 | "내 클리닉" hero | ✅ | 분홍 카드(`primary-light`) + 시간 pill + 성실도 progress + 공부기록 primary pill 버튼 |
| 10.1-3 | 보고서 검정 hero | ✅ | hero(메타+점수+4 progress, rate<60=red) + 회색 코멘트 카드 + 통계 카드 3개(accent=분홍) + ⬇ PDF 아이콘 헤더 |
| 10.1-4 | 영상 카드 | ✅ | 16:9 검정 + 큰 ▶ SVG 가운데, hover 어두움, 인증 뱃지 메타 인라인 |
| 10.1-5 | 과목 카드 | ✅ | enrolled=검정/muted=surface 회색, 책 SVG + 'Ax'·'(x)' glyph, multi-select 제거 → `Fragment` 로 과목별 그룹 노출 |
| 10.1-6 | 최근 본 반 dot | ✅ | `utils/lastClass.js` (localStorage + `ams:last-class-changed` 이벤트), 홈/클리닉/기록 카드 클릭 시 기록, accent 카드 + 빨간 dot |
| 10.1-7 | 클리닉 요일 그룹 | ✅ | `WEEKDAY_ORDER` 그룹핑, 시간 pill 토글(임시 선택), 같은 요일·같은 시간 자동 배제, "예약 저장" 버튼(변경 없으면 disabled), 학생만 적용 — 담임/조교는 기존 즉시-편집 UI 유지 |

## Phase 10.2 — 목업 정합 보강 (backend 의존, 예정)

| 슬라이스 | 항목 | 상태 | 비고 |
|----------|------|------|------|
| 10.2-1 | `ClassResponse` 확장 | 🔲 | `homeroomTeacherName`, `nextScheduleSummary` (요일·시작시각) |
| 10.2-2 | 학생 홈 카드 메타 | 🔲 | "화·목 18:00 · 김선생님" 적용 |
| 10.2-3 | 반 카드 새 알림 dot | 🔲 | `ClassResponse.hasUnread` 연동 |

## Phase 11 — 운영 (2026-05-26)

| 슬라이스 | 항목 | 상태 | 비고 |
|----------|------|------|------|
| 11-2 | V17 `audit_log`, `class.status`, `class_record_archive` | 🟡 | Flyway만 |
| 11-2 | 감사 로그·`DELETE /admin/classes/{id}` | 🔲 | |
| 11-1 | E2E·Playwright | 🔲 | |
| 11-3 | rate limit·magic byte | 🔲 | |
| 11-4 | React Query | 🔲 | |
| 11-5 | Testcontainers | 🔲 | |

**이관:** 구(Phase 10) 운영 항목 → Phase 11. V17 SQL 주석 Phase 11.

## Phase 9 — UI (2026-05-24)

| 항목 | 상태 | 비고 |
|------|------|------|
| 학생 홈 과목 pill | ✅ | 수강 과목만 선택, 미수강 비활성 |
| 학생 홈 인사 | ✅ | `{이름} 학생` |
| 학생 반 홈 탭 | ✅ | §7-2 요약 카드 |
| 선생님·조교 대시보드 | ✅ | §5-1·5-2 |
| 행정 홈 | ✅ | 학원 공지 관리 중심 |
| 학생 하단 탭 | ✅ | 홈·클리닉·기록·MY |

## Phase 8 — 알림 (2026-05-24) ✅

| 항목 | 상태 | 비고 |
|------|------|------|
| V16 `notification`, `device_token` | ✅ | |
| `GET/PATCH /notifications` | ✅ | |
| 학생 트리거 11종 | ✅ | §06-notifications |
| D-1 09:00 스케줄 | ✅ | 숙제·테스트 |
| 토 23:00 클리닉 LOCK + 확정 | ✅ | `NotificationScheduler` |
| UI | ✅ | 🔔 + 목록 |
| FCM | 🔲 | API 미구현 |

## Phase 3 — 조교 담당 반 (2026-05-21)

| 항목 | 상태 | 비고 |
|------|------|------|
| V11 `assistant_class_assignment` | ✅ | |
| `GET/PUT /classes/{id}/assistants` | ✅ | 담임·관리자 배정 |
| `GET /classes` 조교 담당 반 | ✅ | `ClassQueryService` |
| 반 상세 접근 | ✅ | `ClassAccessService` |
| UI | ✅ | 반 상세 「조교」 탭 (담임·관리자) |

## Phase 4 — 영상·인증 (2026-05-21)

| 항목 | 상태 | 비고 |
|------|------|------|
| YouTube oEmbed | ✅ | 등록·수정 시 `thumbnail_url` |
| V12 `video_certification` | ✅ | `POST /videos/{id}/certifications` |
| 로컬 스토리지 | ✅ | `uploads/{academy}/{student}/{date}/` |
| 학생 인증 UI | ✅ | 영상 탭 |
| 공부기록 연동 | ✅ | Phase 6 — 실시간 집계 API·UI |

## Phase 6 — 공부기록 (2026-05-21)

| 항목 | 상태 | 비고 |
|------|------|------|
| 실시간 집계 | ✅ | `StudyRecordService` — 기존 테이블 조회 (로그 테이블 없음) |
| `GET …/study-records/me` | ✅ | 학생 |
| `GET …/study-records/students` | ✅ | 담임·관리자 (학생 목록) |
| `GET …/study-records/students/{id}` | ✅ | 교사·관리자 |
| 종합 % | ✅ | 숙제 40 + 클리닉 30 + 테스트 percentile 평균 30 ([DECISIONS](../planning/DECISIONS.md) §9) |
| 테스트 UI | ✅ | 막대=상대 성적(percentile), 보조=`응시/마감`, 미응시 0% 포함 |
| UI | ✅ | 반 상세 「공부기록」·학생 홈 게이지 |

## Phase 7 — 성실도 보고서 (2026-05-21)

| 항목 | 상태 | 비고 |
|------|------|------|
| V14 `diligence_report` | ✅ | `test_id`+`student_id` UK |
| V15 nullable rate/grade | ✅ | 집계 0건 → NULL (N/A) |
| 트리거 | ✅ | ~~점수 저장 시 자동~~ → **보고서 탭 수동** `POST …/reports/generate/{testId}` (2026-05-26) |
| 구간 | ✅ | 이전 완료 시험 `test_at` ~ 이번 `completed_at` (첫 시험: 수강 배정일) |
| 종합 | ✅ | **유효 항목만** 40/30/30 재가중 (0건 숙제·클리닉은 제외) |
| API | ✅ | `GET /classes/{id}/reports`, `GET/PATCH /reports/{id}`, PDF |
| 권한 | ✅ | 학생·담임·관리자 / 조교 ✗ |
| PDF | ✅ | Apache PDFBox (`uploads/reports/`) — 한글 제한적 |
| 스키마 패치 | ✅ | `DiligenceReportSchemaPatch` (V15 미적용 DB 보정) |
| UI | ✅ | 반 상세 「보고서」 탭 (조교 제외) |

## Phase 5 — 클리닉 예약 (2026-05-21)

| 항목 | 상태 | 비고 |
|------|------|------|
| V13 `clinic_week`, `clinic_reservation` | ✅ | |
| `GET …/clinic/weeks/{weekStart}` | ✅ | 슬롯·예약·주차 상태 |
| `PUT …/reservations` / `cancel` | ✅ | 학생 예약·취소 |
| 토 23:00 마감 (Asia/Seoul) | ✅ | `ClinicBookingPolicy` |
| `PATCH /clinic/reservations/{id}/result` | ✅ | 담임·조교·관리자 |
| 학생 동일 시각 1예약 | ✅ | 요일·시각 충돌 검증 |
| Cron 자동 잠금 | 🔲 | 수동·시간 검증만 |

## Phase 6 — 공부기록 (2026-05-21)

| 항목 | 상태 | 비고 |
|------|------|------|
| 실시간 집계 | ✅ | `study_activity_log` 없음 — 기존 테이블 조회 |
| API | ✅ | `GET …/study-records/me`, `…/students`, `…/students/{id}` |
| 종합 게이지 | ✅ | 숙제 40% + 클리닉 30% + 테스트 percentile 평균 30% |
| 테스트 UI | ✅ | 막대=상대 성적 %, 보조=`응시 n/마감 m` (숙제·클리닉은 n/m %) |
| UI | ✅ | 반 상세 「공부기록」, 학생 홈 종합 % 배지 |

## Phase 7 — 성실도 보고서 (2026-05-21)

| 항목 | 상태 | 비고 |
|------|------|------|
| V14 `diligence_report` | ✅ | 시험·학생 UK |
| V15 nullable rate/grade | ✅ | 집계 0건 → N/A |
| 트리거 | ✅ | 보고서 탭 수동 `POST …/reports/generate/{testId}` (2026-05-26) |
| 구간 | ✅ | 이전 완료 시험 `test_at` ~ 이번 `completed_at` (첫 시험: 수강 배정일~) |
| 종합 | ✅ | 유효 항목만 40/30/30 재가중 (영상은 보고서 항목만) |
| API | ✅ | `GET …/reports`, `GET/PATCH /reports/{id}`, PDF |
| 권한 | ✅ | 학생·담임·관리자 / 조교 ✗ |
| PDF | ✅ | PDFBox, `uploads/reports/` |
| UI | ✅ | 반 상세 「보고서」 탭 (조교 제외) |
| 스키마 패치 | ✅ | `DiligenceReportSchemaPatch` (V15 미적용 DB 보정) |

## Phase 1 — 완료로 본 범위 (MVP)

| 항목 | 상태 | 비고 |
|------|------|------|
| DB `class`, `class_enrollment` (V2) | ✅ | |
| 관리자 반 생성·목록 | ✅ | Create + List (전체 CRUD 아님) |
| 관리자 학생 수강 배정·해제 | ✅ | |
| 관리자 교직원 승인(역할·과목) | ✅ | Phase 0 연계 |
| `GET /api/v1/classes` 역할별 목록 | ✅ | 담임·학생·관리자 |
| 관리자·학생·담임 홈 UI | ✅ | 조교·행정은 목록 빈 상태 안내 |
| 미배정 학생 반 목록 | ✅ | 빈 배열 + 배정 대기 UI |

## Phase 1 — 의도적 제외 (다른 Phase)

| 항목 | 이관 | 이유 |
|------|------|------|
| 반 **삭제**·아카이브 | Phase 11 | 운영·감사 요구 |
| 반 **수정** (이름·담임·강의실) | Phase 1.5 → **2026-05-21 반영** `PATCH /admin/classes/{id}` | 운영 오타 수정 |
| 배정 완료 **알림 이벤트** | Phase 8 (FCM과 함께) | 알림 인프라 없이 스텁만 두지 않음 |
| 미배정 학생 `GET /classes/{id}` **403** | Phase 2 | 상세 API 도입 시 검증 |
| 조교 **담당 반** 목록 | ✅ Phase 3 | `assistant_class_assignment` |
| 반 상세·7섹션 | Phase 2 | 본 문서 §Phase 2 슬라이스 |

## Phase 1.5 (2026-05-21)

| 항목 | API |
|------|-----|
| 반 수정 | `PATCH /api/v1/admin/classes/{id}` |

## Phase 2 — 슬라이스 계획

| 슬라이스 | DB (Flyway) | API | UI | 상태 |
|----------|-------------|-----|-----|------|
| **2-1 공지** | V3 `class_notice` | `GET /classes/{id}`, `GET/POST …/notices` | 반 상세·공지 탭 | ✅ 2026-05-21 |
| 2-2 수업정보 | V4 `class_schedule` | `GET/PATCH …/schedule` | 수업정보 탭 | ✅ 2026-05-21 |
| 2-3 교재 | V5 `textbook` | `GET/PATCH …/textbook` | 교재 탭 (반당 1권, 기획서 정합) | ✅ 완료 |
| 2-4 숙제·테스트 | V6 | `homeworks`·`tests` + 학생별 결과 | 숙제·테스트 탭 | ✅ 2026-05-21 |
| 2-5 영상 | V7 `video_lesson` | `GET/POST/PATCH/DELETE …/videos` | 영상 탭 | ✅ 2026-05-21 |
| 2-6 클리닉 슬롯 | V8 `clinic_slot` | `GET/POST/PATCH/DELETE …/clinic/slots` | 클리닉 탭 | ✅ 2026-05-21 |
| 2-7 학원 공지 | V10 `academy_notice` | `GET/POST /academy/notices` | 홈(행정·관리자·전체 열람) | ✅ 2026-05-21 |

**원칙:** 한 슬라이스 = 마이그레이션 + API + 최소 UI → 데모 가능 후 다음 슬라이스.

## 읽기 순서 (일일 작업)

1. [STATUS.md](./STATUS.md) — 지금 Phase·오늘 할 일  
2. **본 문서** — 완료/제외/슬라이스  
3. [ROADMAP.md](../planning/ROADMAP.md) — 전체 체크리스트  
