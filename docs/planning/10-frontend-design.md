# 10. 프론트엔드 디자인 전면 개편 (Phase 10)

**목표:** `docs/design/DESIGN_SYSTEM.md` 및 `reference/` 목업과 **전 화면** 시각·UX 정합. Phase 9에서 붙인 기능 UI를 **디자인 시스템 기준으로 재구성**한다.

- 참조: [DESIGN_SYSTEM.md](../design/DESIGN_SYSTEM.md) · [05-screens-by-role.md](./05-screens-by-role.md)
- 구현: `ams_frontend/` — 토큰·공통 컴포넌트 우선, 역할·화면별 순차 적용
- **백엔드 API 변경 없음** (필요 시 버그·문구만). 운영·E2E·V17은 **Phase 11**.

## 완료 기준

1. Vite 기본 테마(보라 `#646cff` 등) **완전 제거**, `--ams-*` 토큰만 사용
2. 역할별 주요 플로우가 reference 목업과 **레이아웃·색·컴포넌트** 수준에서 일치
3. 모바일 우선(하단 탭 4), 태블릿·데스크톱 **기본 반응형** 동작
4. 포커스·대비·터치 영역 등 **접근성 1차** 점검 체크리스트 통과

## 슬라이스 (권장 순서)

| ID | 범위 | 산출물 | ROADMAP 연계 |
|----|------|--------|----------------|
| **10-1** | 토큰·글로벌·레이아웃 | `design-tokens.css`, `index.css`, `App.css`, `AppLayout`, `AuthLayout`, `BottomNav` | 공통 셸 |
| **10-2** | 인증·온보딩 | `LoginPage`, `Signup*`, `PendingApprovalPage`, `auth.css` | Phase 0 화면 |
| **10-3** | 학생 — 홈·탭·MY | `StudentHomePage`, `StudentClinicPage`, `StudentRecordsPage`, `StudentMyPage`, `student.css`, `bottom-nav.css` | Phase 9-1·9-2·9-6 |
| **10-4** | 학생 — 반 상세 7섹션 | `ClassDetailPage`, `Class*Section`, `class-detail.css` | Phase 2·9 미완 탭 연동 |
| **10-5** | 교직원 홈·알림 | `StaffHomePage`, `AdminHomePage`, `NotificationsPage`, `NotificationBell`, `AcademyNoticesSection` | Phase 8·9-3~9-5 |
| **10-6** | 관리자·반 운영 UI | `AdminClassesSection`, `admin.css`, `class-list.css` | Phase 1 관리자 |
| **10-7** | 공통 컴포넌트·카드 | `DashboardCard` 등 — 중복 스타일 제거, 패턴 문서화 | DESIGN_SYSTEM §컴포넌트 |
| **10-8** | 반응형·접근성·회귀 | 브레이크포인트, focus-visible, 라벨·aria, 스모크 체크리스트 | ROADMAP Phase 9 잔여 `[ ]` |

## Phase 9와의 관계

| Phase 9 (완료) | Phase 10 (본 Phase) |
|----------------|---------------------|
| API 연동·기능 동작 | 시각·레이아웃·컴포넌트 통일 |
| MVP 홈·탭 | 목업 수준 polish |
| 일부 섹션·a11y 미완 | 10-4·10-8에서 마감 |

## Phase 10.1 — 목업 정합 보강 (2026-05-26)

10-1 ~ 10-8 으로 토큰·셸·역할별 골격을 끝낸 뒤, `reference/01-home-clinic.png` · `02-clinic-reservation-report.png` 와의 갭을 마무리하는 추가 슬라이스.

> **백엔드 비의존**: 모든 작업은 frontend 만으로 수행. backend 필드 추가(시간·담임명 등)는 **Phase 10.2** 로 별도 분리.

| ID | 범위 | 산출물 |
|----|------|--------|
| **10.1-1** | 클래스 상세 헤더 가운데 정렬 + ⋮ 메뉴 placeholder | `ClassDetailPage`, `class-detail.css` (3-col grid 헤더) |
| **10.1-2** | "내 클리닉" 통합 카드 (분홍 hero · 시간 pill · 성실도 progress · 공부기록 pill 버튼) | `ClassHomeSection`, `.ams-class-home__highlight*` |
| **10.1-3** | 성실도 보고서 검정 hero 카드 + 4 progress + 통계 카드 3개 + ⬇ PDF 아이콘 | `ClassReportsSection`, `.ams-report-modal__hero/metrics/stats` |
| **10.1-4** | 영상 카드 검정 16:9 + 큰 가운데 ▶ 오버레이 | `ClassVideoSection`, `.ams-video-list__player/play` |
| **10.1-5** | 학생 홈 과목 카드 수강중=검정 / 미수강=회색, outline SVG 아이콘, multi-select 제거 → 과목별 그룹 노출 | `StudentHomePage`, `.ams-subject-cards__item--enrolled/muted` |
| **10.1-6** | 학생 홈 "최근 본 반" surface 강조 + 빨간 dot (`localStorage`) | `StudentHomePage/ClinicPage/RecordsPage`, `utils/lastClass.js`, `.ams-class-list__item--accent` |
| **10.1-7** | 클리닉 예약 요일 그룹핑 + 시간 pill + 명시적 "예약 저장" 버튼 (임시 선택 모드) | `ClassClinicSection`, `.ams-clinic-day-list*` |

### 작업 결정 (Phase 10.1)

| 항목 | 결정 |
|------|------|
| 반 상세 ⋮ 메뉴 | (a) **빈 placeholder** (disabled, title="추가 메뉴 (준비 중)") |
| 기록 탭 구조 | (A) **반 리스트 유지** → 클릭 시 `?tab=study` 풀스크린 보고서 |
| 클리닉 예약 저장 | (Y) **명시적 저장 버튼** — 임시 선택 후 일괄 commit |

### Phase 10.2 (backend 의존) — 후속

| 항목 | 백엔드 |
|------|--------|
| 학생 홈 반 카드 "화·목 18:00 · 김선생님" | `ClassResponse` 에 `homeroomTeacherName` + `nextScheduleSummary` |
| 반 카드 새 알림 dot | `ClassResponse.hasUnread` |

## 하지 않는 것 (Phase 11로 이관)

- 감사 로그·반 아카이브 API (V17)
- E2E Playwright, Testcontainers
- 로그인 rate limit, magic byte
- React Query (캐싱 — 디자인 안정 후 11-4 권장)
- FCM · PDF 한글

## 작업 원칙

1. **화면 단위 PR** — 슬라이스별로 데모 가능하게 머지
2. **신규 hex 금지** — `design-tokens.css`만 수정
3. 목업에 없는 UI는 DESIGN_SYSTEM 패턴(카드·pill·primary CTA)으로 파생
4. 기능 회귀 없이 — 기존 라우트·API 호출 유지

Phase 체크리스트: [ROADMAP.md](./ROADMAP.md) Phase 10 · 진행: [STATUS.md](../progress/STATUS.md)
