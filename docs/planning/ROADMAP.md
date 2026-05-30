# AMS 구현 로드맵

기획서 v2.0 전 기능을 단계별로 구현하기 위한 실행 계획.

- 요구사항: `01`~`10`
- 확정 스펙: [DECISIONS.md](./DECISIONS.md)
- 인덱스: [README.md](./README.md)
- **일일 진행:** [STATUS.md](../progress/STATUS.md) · [daily/](../progress/daily/)

## 목표 정의

| 항목 | 내용 |
|------|------|
| 제품 | 입시 학원 통합 관리 플랫폼 (반 단위, 국·수·영) |
| 플랫폼 | 웹 + 모바일 반응형 |
| 사용자 | 관리자, 담임, 조교, 학생, **학부모** (v3.0) |
| 완료 기준 | 기획서 [03-feature-matrix.md](./03-feature-matrix.md)의 기능이 역할별로 동작하고, E2E 시나리오 통과 |

## 구현 원칙

1. **학원(테넌트) 단위 격리** — 모든 데이터에 `academy_id` 적용
2. **RBAC + 반 단위 접근** — 학생은 배정된 반만, 조교는 담당 반만
3. **백엔드 우선** — API·권한·스케줄러 확정 후 프론트 연결
4. **단계별 배포 가능** — 각 Phase 종료 시 데모 가능한 수준 유지

## 기술 스택 (현 프로젝트 기준)

| 영역 | 선택 |
|------|------|
| API | Spring Boot 4 (`ams_backend`), REST, Spring Security |
| DB | MySQL 8.0, Flyway/Liquibase 마이그레이션 권장 |
| ORM/DB 접근 | JdbcTemplate 또는 MyBatis 중 **하나로 통일**(현재 JDBC 사용 중) |
| 프론트 | React + Vite (`ams_frontend`), 역할별 라우트·레이아웃 |
| 인증 | JWT + Refresh (OAuth는 Phase 0 이후 검토) |
| 파일 | S3 호환 스토리지(인증사진), 로컬은 개발용 |
| 알림 | FCM + 스케줄러(Spring `@Scheduled` / Quartz) |
| PDF | Apache PDFBox (성실도 보고서) |
| 영상 메타 | YouTube oEmbed API |

---

## Phase 0 — 기반 구축 (2~3주)

**목표:** 멀티 테넌트·인증·권한·프로젝트 골격

### 백엔드

- [x] 패키지 구조: `domain`, `api`, `service`, `repository`, `config`, `security`
- [x] DB 마이그레이션 도입(Flyway 권장)
- [x] 테이블: `academy`, `user` (역할은 `user.role` enum)
- [x] 회원가입 API (교직원 7역할 / 학생 / 학원 개설)
- [x] 학원 코드 검증, 원장 승인(교직원 활성화) — 승인 시 역할·과목 **확정/수정** ([DECISIONS](./DECISIONS.md) §1)
- [x] JWT 발급·갱신
- [x] 공통 응답·예외 처리, OpenAPI(SpringDoc) 문서화
- [x] Row-level: `CurrentUserService`·`AcademyAdminInterceptor` (`academy_id`·관리자 역할, Phase 1부터 반 소속 확장)

### 프론트

- [x] `src/pages`, `src/api`, `src/auth`, `src/router` 구조
- [x] 로그인·회원가입·승인 대기 화면
- [x] 역할별 레이아웃 라우팅 골격(관리자/선생님/조교/학생)
- [x] API 클라이언트(fetch + 토큰), Vite proxy `/api`

### 산출물

- 로그인 후 역할에 맞는 빈 홈 화면 진입
- [database/schema-overview.md](./database/schema-overview.md) Phase 0 테이블 반영

---

## Phase 1 — 관리자·반·수강 배정 (2~3주)

**목표:** 반 생성, 담임 지정, 학생 수강반 배정

### 기능 (기획서 §2-1, §2-4, 권한 매트릭스)

- [x] 반 생성·목록·수정(PATCH) — 삭제는 Phase 11 ([PHASE-SCOPE](../progress/PHASE-SCOPE.md))
- [x] 학생 목록(학원 전체), 수강반 배정(`class_enrollment`)
- [x] 배정 전 학생: 목록 빈 배열; `GET /classes/{id}` 미배정 시 403 (Phase 2-1)
- [ ] 배정 완료 알림(이벤트만, FCM은 Phase 8) → Phase 8
- [x] 선생님 가입 시 과목·역할 지정(관리자 승인)
- [x] `GET /classes` 역할별 반 목록

### 화면

- [x] 관리자: 학생 목록, 반 목록, 반 생성, 수강 배정 UI
- [x] 학생: 배정 대기 상태 UI

### DB 추가

- `class`, `class_enrollment`, `subject` (enum 또는 코드 테이블)

---

## Phase 2 — 반 7섹션 CRUD (3~4주)

**목표:** 반 상세의 수업정보·공지·교재·영상·숙제·테스트·클리닉 **데이터 모델 + 담임/조교 편집**, 학원 공지([DECISIONS](./DECISIONS.md) §2)

### 공통

- [x] 반 상세 API: `GET /api/v1/classes/{id}` + 접근 검증 (Phase 2-1)
- [ ] 섹션별 CRUD 전체 — **2-1 공지** ✅, 나머지 슬라이스 진행 ([PHASE-SCOPE](../progress/PHASE-SCOPE.md))

### 학원 공지 (`academy_notice`)

- [ ] API: `GET/POST/PATCH /api/academy/notices` (행정 `STAFF_OFFICE`, `ACADEMY_ADMIN`)
- [ ] 행정·관리자: 학원 공지 작성·수정 UI
- [ ] 교직원·학생 대시보드: 학원 공지 목록(읽음 처리는 Phase 8과 연동 가능)

### 섹션별

| 섹션 | API·비고 |
|------|----------|
| 수업 정보 | 요일·시간·강의실, 담임만 수정 |
| 공지 (`class_notice`) | 제목·내용·첨부·예약 발송(스케줄 필드) |
| 교재 | 제목·출판사·진도 |
| 영상 | URL 저장(썸네일·제목은 Phase 4) |
| 숙제 | 예정/완료 + 학생별 submission ([DECISIONS](./DECISIONS.md) §5) |
| 테스트 | 예정/완료 + 학생별 score ([DECISIONS](./DECISIONS.md) §5) |
| 클리닉 | 슬롯 마스터(요일·시간·조교), Phase 5에서 예약 연동 |

### 화면

- [ ] 담임: 반 상세 탭 UI (편집 모드)
- [ ] 조교: 담당 반만 목록·동일 탭(권한 제한)
- [ ] 행정: 학원 공지 관리 화면(반 상세 탭 없음)

### DB

- [04-class-data-model.md](./04-class-data-model.md) 테이블 전부 생성 + `academy_notice`

---

## Phase 3 — 조교 담당 반 지정 (1주)

**목표:** 담임 → 조교 반 매핑

- [ ] `assistant_class_assignment` (조교 user_id, class_id)
- [ ] 담임: 조교 목록 + 반 체크 UI (기획서 5-3)
- [ ] 조교 API는 담당 `class_id`만 허용

---

## Phase 4 — 영상 수업·인증 (2주)

**목표:** YouTube 연동 + 학생 인증사진

- [ ] oEmbed 연동: URL → 썸네일·제목 저장
- [ ] 비공개 영상 fallback UI
- [ ] 학생: 유튜브 링크, [인증사진 제출] (카메라/앨범 → 업로드 API)
- [ ] 스토리지 경로: `{academy_id}/{student_id}/{date}/`
- [x] `video_certification` 로그 → 공부기록 반영(Phase 6)

---

## Phase 5 — 클리닉 예약·잠금 (2~3주)

**목표:** 주간 예약, 토 23:00 잠금, 월 00:00 슬롯 생성

- [ ] 슬롯 CRUD (담임): 요일·시간·담당 조교, `max_capacity`(정원·마감)
- [ ] 학생: 주간 예약표, 변경(토 23:00 전), 정원 초과 시 예약 불가
- [ ] Cron: 토 23:00 `LOCK` + 확정 알림 이벤트
- [ ] Cron: 월 00:00 다음 주 슬롯 생성
- [ ] **담임·조교**: 신청 목록 확인, **클리닉 결과 입력**(출석·메모) — 매트릭스 ✓
- [ ] 서버 시간대: `Asia/Seoul` 고정

---

## Phase 6 — 공부기록·게이지 (2주) ✅

**목표:** 실시간 집계, 반 홈 게이지, 학생 상세

- [x] 이벤트 수집: 숙제 제출, 클리닉 출석, 테스트 점수, 영상 인증 (기존 테이블 실시간 집계)
- [x] 종합 %: 숙제 40% + 클리닉 30% + 테스트 30% (테스트 환산: [DECISIONS](./DECISIONS.md) §9, 영상 별도: §7)
- [x] 색상 규칙: 75+/50–74/50 미만
- [x] API: `GET /api/v1/classes/{id}/study-records/me`, `…/students`, `…/students/{sid}`
- [x] 학생 홈·반 상세 공부기록 탭: 종합 %, 게이지 UI

---

## Phase 7 — 성실도 보고서 (2~3주) ✅

**목표:** 테스트 결과 입력 시 구간 보고서 자동 생성·PDF

- [x] 트리거: 테스트 `결과 입력` → 이전 테스트~현재 구간 집계
- [x] 보고서 항목: 숙제/클리닉/테스트/영상/종합/등급(A~F)
- [x] 담임 코멘트 입력
- [x] PDF 생성·다운로드 API (PDFBox)
- [x] 구간 집계 0건: 달성률·등급 N/A (V15), 종합은 유효 항목만 가중
- [x] 권한: 학생 본인, 담임 반, 관리자 전체 / 조교 ✗

---

## Phase 8 — 알림 시스템 (2~3주)

**목표:** 기획서 §8 전 유형

- [x] `notification` 테이블 (+ `device_token` 스키마)
- [ ] FCM 토큰 등록(웹: optional, 모바일 대비)
- [x] 트리거: 공지·영상·숙제·테스트 등록, 결과 입력, 배정, 보고서, 클리닉
- [x] 스케줄: 숙제·테스트 D-1 오전 9시, 클리닉 토 23:00 LOCK
- [ ] **수신 대상:** 교직원 알림 (학생 ✅)
- [x] 학생·교직원 홈 🔔 뱃지, 목록(전체/미확인)

---

## Phase 9 — 선생님·조교·학생 화면 완성 (3~4주)

**목표:** 기획서 §5~7 와이어 수준 UI를 실제 데이터로 완성

- [x] 선생님 대시보드: 담임 반, 주간 일정, 미확인 알림
- [x] 조교 홈: 담당 반, 클리닉 현황
- [x] **행정** 홈: 학원 공지 관리, 학원 공지 목록·알림(반 운영 탭 없음)
- [x] 학생: 과목 선택(수강 과목만), 반 목록
- [x] 학생: 반 홈 요약 (홈 탭)
- [ ] 학생: 반 홈 전 섹션 (영상·교재 등 탭 연동) → **Phase 10** 슬라이스 10-4
- [ ] 반응형·접근성 기본 점검 → **Phase 10** 슬라이스 10-8

---

## Phase 10 — 프론트엔드 디자인 전면 개편 (3~4주)

**목표:** [10-frontend-design.md](./10-frontend-design.md) · [DESIGN_SYSTEM.md](../design/DESIGN_SYSTEM.md)

- [x] 10-1 토큰·글로벌·레이아웃 (`design-tokens`·`components.css`·`AppLayout`·`BottomNav`)
- [x] 10-2 인증·온보딩 화면 (`Login`, `Signup*`, `PendingApproval`)
- [x] 10-3 학생 홈·하단 탭·클리닉·기록·MY
- [x] 10-4 반 상세 7섹션 UI 통일 (헤더·탭·클리닉·보고서)
- [x] 10-5 교직원·행정 홈·알림 UI (`admin.css` 재작성, `AcademyNotices` 이관, `NotificationBell` SVG)
- [x] 10-6 관리자 반·학원 공지 UI (`AdminClassesSection` 반 카드/폼 정합)
- [x] 10-7 공통 컴포넌트·카드 패턴 정리 (미사용 `App.css`/`student.css` 삭제, study-record 진행바 토큰 통일)
- [x] 10-8 반응형·접근성·스모크 회귀 (탭 가로 스크롤(모바일)/wrap(≥768px), container `overflow-x: hidden`, focus-visible)

### Phase 10.1 — 목업 정합 보강 (frontend only, 2026-05-26)

> 참조: [10-frontend-design.md §10.1](./10-frontend-design.md). 두 PNG 목업 100% 정합 + UX 결정 3건.

- [x] 10.1-1 반 상세 헤더 3-col grid (← / 가운데 / ⋮ placeholder)
- [x] 10.1-2 "내 클리닉" 분홍 hero (시간 pill + 성실도 progress + 공부기록 pill)
- [x] 10.1-3 성실도 보고서 검정 hero + 통계 카드 3개 + ⬇ PDF 아이콘
- [x] 10.1-4 영상 카드 검정 16:9 + 큰 ▶ 오버레이
- [x] 10.1-5 과목 카드 수강중=검정/미수강=회색 + 책 SVG·Ax·(x) (multi-select 제거)
- [x] 10.1-6 학생 홈 "최근 본 반" surface + 빨간 dot (`utils/lastClass.js`)
- [x] 10.1-7 클리닉 요일 그룹 + 시간 pill + 명시적 "예약 저장" 버튼

### Phase 10.2 — 목업 정합 (backend 의존, 예정)

- [ ] `ClassResponse` 에 `homeroomTeacherName` + `nextScheduleSummary` (요일·시작시각) 노출
- [ ] 학생 홈 반 카드 메타 "화·목 18:00 · 김선생님" 적용
- [ ] `ClassResponse.hasUnread` 로 반 카드 새 알림 dot

---

## Phase 12 — v3.0 기획 개정 (4~6주)

**목표:** [DECISIONS.md](./DECISIONS.md) §10~§19 — 수업기록·정오표·재시험·학부모

> **확정일:** 2026-05-30. Phase 0~10 완료 위 **마이그레이션·리팩터**. **Phase 11(운영)보다 먼저** 진행.

### 12-1. 수업기록 (§10)

- [x] `lesson_record` · 반 상세 **수업기록 탭**
- [x] 숙제·테스트·클리닉·영상 FK 컬럼 (nullable)
- [ ] 숙제·테스트 등록 API `lesson_record_id` 연동
- [x] 공지·일정·교재 별도 유지

### 12-2. 정오표·채점 (§11)

- [ ] 문항별 answer_key · answers JSON
- [ ] `ceil(100/q×correct,1)` · `due_at` 제거
- [ ] 교직원 전용 · 조교 정답지 조회

### 12-3. 석차·재시험 (§12, §13)

- [ ] `rank` · 동점 공동·건너뛰기
- [ ] 재시험 3회 · 올림/내림 · 최종=마지막
- [ ] percentile deprecated

### 12-4. 대상 학생·영상 (§14)

- [ ] `assignment_target` · 영상 기본 미선택

### 12-5. 조회 시작일 (§15)

- [ ] `accessible_from` · 클리닉 제외

### 12-6. 알림 (§16)

- [ ] ACTIVE/DISMISSED · D-1 제거 · 홈 미완료

### 12-7. 클리닉 프리셋 (§17)

- [ ] preset · start_time only · capacity 10

### 12-8. 보고서 (§18)

- [ ] period preset · 학생 선택 · ZIP

### 12-9. 학부모 (§19)

- [ ] `PARENT` · parent-links · `/parent/*`

DB: V21~V26 ([schema-overview](./database/schema-overview.md))

---

## Phase 11 — 비기능·운영 (2주) **← 최종 Phase**

**목표:** [10-technical-requirements.md](./10-technical-requirements.md)

> **실행 시점:** **Phase 12 (v3.0) 완료 후** 맨 마지막. v3.0 기능 안정화·배포 후 E2E·보안·캐싱·감사 로그.

> **코드 기준 (2026-05-26):** 구현 롤백됨. `V17__audit_log_class_archive.sql`만 잔존. 상세는 [STATUS.md](../progress/STATUS.md).

- [ ] 감사 로그 (`audit_log` 기록 API·서비스)
- [ ] 관리자만 반 삭제(아카이브) — `DELETE /admin/classes/{id}` 등
- [ ] 반 삭제 시 학생 기록 아카이브 (`class_record_archive` 스냅샷)
- [x] DB 스키마 초안 (V17) — **API 미연동**
- [ ] 주요 반 콘텐츠 캐싱(프론트 React Query 등)
- [ ] 부하·보안: 로그인 rate limit, JPEG/PNG magic byte
- [ ] E2E 테스트 시나리오 문서 + Playwright 골격
- [ ] API 통합 테스트 (MySQL Testcontainers)

---

## 전체 일정 요약 (참고)

| Phase | 기간(참고) | 누적 |
|-------|------------|------|
| 0 기반 | 2~3주 | 3주 |
| 1 관리자·반 | 2~3주 | 6주 |
| 2 반 7섹션 | 3~4주 | 10주 |
| 3 조교 지정 | 1주 | 11주 |
| 4 영상·인증 | 2주 | 13주 |
| 5 클리닉 | 2~3주 | 16주 |
| 6 공부기록 | 2주 | 18주 |
| 7 보고서 | 2~3주 | 21주 |
| 8 알림 | 2~3주 | 24주 |
| 9 UI 완성 | 3~4주 | 28주 |
| 10 디자인 개편 | 3~4주 | 32주 |
| **12 v3.0** | **4~6주** | **38주** |
| **11 운영 (최종)** | 2주 | **약 40주** |

> **실행 순서:** Phase 0~10 → **10.2 (선택)** → **12 v3.0** → **11 운영 (맨 마지막)**

1인 개발 기준 참고치이며, 인원·MVP 범위에 따라 조정합니다.

---

## MVP 제안 (빠른 검증용, Phase 0~2 + 5 일부)

데모·학원 파일럿용 최소 범위:

1. 로그인 + 학원 코드 + 관리자 승인
2. 반 생성 + 학생 배정
3. 공지·숙제·테스트(등록만)
4. 클리닉 슬롯 + 학생 예약(잠금 포함)

영상 인증·보고서·FCM·PDF는 MVP 이후 Phase 순서대로 추가.

---

## 다음 액션 (즉시)

> 상세·당일 할 일은 **[STATUS.md](../progress/STATUS.md)** 에서 관리.

**Phase 2 진행 (2026-05-21):**

1. ~~`V3` `class_notice` + `GET /classes/{id}` + 공지 API/UI~~ ✅ (슬라이스 2-1)
2. ~~슬라이스 2-2: `class_schedule` + 수업정보 탭~~ ✅
3. ~~슬라이스 2-3: 교재 (`textbook`)~~ ✅
4. ~~슬라이스 2-4: 숙제·테스트~~ ✅
5. ~~슬라이스 2-5: 영상 (`video_lesson`)~~ ✅
6. ~~슬라이스 2-6: 클리닉 슬롯~~ ✅
7. ~~슬라이스 2-7: 학원 공지~~ ✅
8. ~~**Phase 3:** 조교 담당 반~~ ✅
9. ~~Phase 4: 영상 oEmbed·인증~~ ✅ (공부기록 연동 Phase 6)
10. ~~Phase 5: 클리닉 예약~~ ✅ (Cron 잠금 제외)
11. ~~Phase 6: 공부기록~~ ✅
12. ~~Phase 7: 성실도 보고서~~ ✅
13. ~~**Phase 8:** 알림 (FCM 제외)~~ ✅
14. ~~**Phase 9:** 역할별 화면 (MVP 홈·탭)~~ ✅ — 반 홈 전 섹션·a11y는 ROADMAP `[ ]` 유지
15. ~~**Phase 10:** 프론트 디자인 전면 개편~~ ✅ (10-1~10-8 + **10.1-1~10.1-7 목업 정합**, [10-frontend-design.md](./10-frontend-design.md))
16. **Phase 10.2 (BE):** `ClassResponse` 확장 → 학생 홈 반 카드 메타
17. **Phase 12 (v3.0):** [DECISIONS](./DECISIONS.md) §10~§19 — 수업기록·정오표·재시험·학부모
18. **Phase 11 (최종):** 운영 — V17 아카이브 API → E2E → rate limit → Testcontainers

---

## 진행 상태 추적

| 문서 | 용도 |
|------|------|
| **[STATUS.md](../progress/STATUS.md)** | 지금 Phase, 이번 주·오늘·다음 할 일, 블로커 (**매일 확인**) |
| **[daily/](../progress/daily/)** | 날짜별 완료·메모 (**퇴근 전 1건**) |
| **본 문서 ROADMAP** | Phase별 체크리스트 (**완료 시 `[x]`)** |

Phase 요약 표는 STATUS.md에 유지한다. 구현 착수·Phase 완료 시 STATUS의 Phase 표를 갱신한다.

사용법: [progress/README.md](../progress/README.md)
