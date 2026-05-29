# AMS 기획 문서

입시 학원 통합 관리 플랫폼(AMS) — 기획서 v2.0 기반 설계·구현 가이드.

## 문서 구조 (3층)

```
[ 원본 ]  입시학원_통합관리앱_기획서.docx (v2.0, 2025.05)
              │
              ▼
[ 요구사항 ]  00 ~ 10 번호 md  ── 기능·화면·규칙·비기능
              │
              ▼
[ 확정 ]      DECISIONS.md     ── 원본 미명시·모순 9건 (구현 스펙)
              │
              ▼
[ 실행 ]      ROADMAP.md       ── 개발 단계·일정·MVP
              ├── database/    ── 테이블 개요
              └── api/         ── REST 초안
[ 디자인 ]    ../design/       ── UI 테마·목업·토큰
[ 진행 ]      ../progress/     ── 일일 STATUS · daily 일지
```

| 읽는 순서 | 문서 | 용도 |
|-----------|------|------|
| 1 | [00-document-metadata.md](./00-document-metadata.md) | 표지·목차 |
| 2 | [01](./01-service-overview.md) ~ [10](./10-technical-requirements.md) | **무엇을** 만드는지 |
| 3 | **[DECISIONS.md](./DECISIONS.md)** | **어떻게 해석·확정**할지 (★구현 전 필수) |
| 4 | [database/schema-overview.md](./database/schema-overview.md), [api/api-catalog.md](./api/api-catalog.md) | 데이터·API |
| 5 | **[ROADMAP.md](./ROADMAP.md)** | **언제** 무엇을 개발할지 |
| ★ | **[../progress/STATUS.md](../progress/STATUS.md)** | **오늘** 무엇을 했고 할지 (매일) |
| ★ | **[../progress/PHASE-SCOPE.md](../progress/PHASE-SCOPE.md)** | Phase **완료/제외/슬라이스** 정의 |
| — | [../design/DESIGN_SYSTEM.md](../design/DESIGN_SYSTEM.md) | **어떻게** 보일지 (UI) |

## 원본 기획서

- 파일: `입시학원_통합관리앱_기획서.docx`
- 로컬 경로: `c:\Users\mhm14\OneDrive\Documents\카카오톡 받은 파일\`

요구사항 본문은 `01`~`10`에 요약·구조화되어 있다. docx와 **글자 단위 일치**는 요구하지 않으며, **기능·정책 구현**이 목표이다.

## 구현 가능성 (요약)

| 질문 | 답 |
|------|-----|
| planning만으로 기획서 전 기능 구현 가능? | **예** — `01`~`10` + `DECISIONS` + `database` + `api` + `ROADMAP` |
| 추가 비즈니스 결정 필요? | **아니오** — [DECISIONS.md](./DECISIONS.md) 9건 확정 완료 |
| 원본과 모순 시 | **DECISIONS.md** 우선 (예: 조교 교재 = 조회만) |

## 요구사항 문서 (01 ~ 10)

| 문서 | 내용 |
|------|------|
| [01-service-overview.md](./01-service-overview.md) | 서비스 개요·목적 |
| [02-roles-and-permissions.md](./02-roles-and-permissions.md) | 역할·권한·RBAC |
| [03-feature-matrix.md](./03-feature-matrix.md) | 기능 권한 매트릭스 |
| [04-class-data-model.md](./04-class-data-model.md) | 반(Class) 7섹션 |
| [05-screens-by-role.md](./05-screens-by-role.md) | 역할별 화면 |
| [06-notifications.md](./06-notifications.md) | 알림 |
| [07-video-certification.md](./07-video-certification.md) | 영상·인증 |
| [08-clinic-rules.md](./08-clinic-rules.md) | 클리닉 예약 |
| [09-study-records-reports.md](./09-study-records-reports.md) | 공부기록·성실도 보고서 |
| [10-technical-requirements.md](./10-technical-requirements.md) | 기술·비기능 (**Phase 11** 운영) |
| [10-frontend-design.md](./10-frontend-design.md) | **Phase 10** UI 전면 개편 실행 |

## 실행 문서

| 문서 | 내용 |
|------|------|
| [DECISIONS.md](./DECISIONS.md) | 구현 확정 9건 (역할·공지·출석·교재·성적·클리닉·영상·숙제·테스트 환산) |
| [ROADMAP.md](./ROADMAP.md) | Phase 0~11, MVP, 체크리스트 |
| [10-frontend-design.md](./10-frontend-design.md) | Phase 10 디자인 전면 개편 |
| [../progress/README.md](../progress/README.md) | 일일 루틴·STATUS·daily 일지 |
| [../progress/STATUS.md](../progress/STATUS.md) | 현재 Phase·이번 주·블로커 (라이브) |
| [database/schema-overview.md](./database/schema-overview.md) | MySQL 테이블 |
| [api/api-catalog.md](./api/api-catalog.md) | REST 엔드포인트 |

## 현재 코드베이스

| 구분 | 경로 | 상태 |
|------|------|------|
| 백엔드 | `ams_backend/` | Spring Boot 4, MySQL |
| 프론트 | `ams_frontend/` | React 19, Vite 8 · [디자인 토큰](../design/DESIGN_SYSTEM.md) |
| DB | MySQL `ams` | Flyway V1~V17 (`ams_backend/.../migration/`) |

개발 착수: [ROADMAP.md](./ROADMAP.md) Phase 0 → Flyway `V1__…` ([database](./database/schema-overview.md) 참고).

## 문서 수정 규칙

1. 기획서 원본과 충돌 시 → 원본 우선 검토 후 **DECISIONS.md** 갱신.
2. 스키마·API 변경 시 → `database/`, `api/` 먼저 수정 후 `01`~`10`·ROADMAP 반영.
3. 확정 사항 추가 시 → **DECISIONS.md**에만 추가 (중복 문서 만들지 않음).

## 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-05-19 | IMPLEMENTABILITY·VERIFICATION → README 통합, 메타 문서 3개로 정리 |
| 2026-05-19 | DECISIONS §9·ROADMAP 감사 권장 반영 (학원 공지·승인·클리닉·알림·행정 UI) |
| 2026-05-19 | UI 디자인 테마 — `docs/design/`, `design-tokens.css`, Cursor rule |
| 2026-05-19 | 일일 진행 추적 — `docs/progress/` (STATUS, daily, README) |
