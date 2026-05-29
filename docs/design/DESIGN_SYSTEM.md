# AMS 디자인 시스템

모바일 학원 앱 UI의 **단일 기준**. 구현·리뷰·AI 보조 시 이 문서와 `reference/` 목업을 따른다.

## 참조 목업

| 파일 | 화면 |
|------|------|
| [01-home-clinic.png](./reference/01-home-clinic.png) | 홈(과목·수강반), 클리닉 상세(성실도·영상) |
| [02-clinic-reservation-report.png](./reference/02-clinic-reservation-report.png) | 클리닉 예약, 성실도 보고서 |

## 디자인 원칙

- **미니멀·고대비**: 흰 배경 + 검정 강조 + 레드 포인트
- **카드 기반**: 넉넉한 여백, 큰 border-radius, 얇은 테두리
- **모바일 우선**: 하단 탭 4개 — 홈 · 클리닉 · 기록 · MY
- **선택 상태**: 연한 핑크/레드 배경 + 레드 테두리 + 레드 pill

## 색상 (토큰)

| 토큰 | 값 | 용도 |
|------|-----|------|
| `--ams-color-primary` | `#C21831` | 활성 탭, 진행바, CTA, 강조 텍스트 |
| `--ams-color-primary-dark` | `#9B1428` | 버튼(공부기록 등) |
| `--ams-color-primary-light` | `#FCE8EC` | 선택 행·카드 배경 |
| `--ams-color-black` | `#000000` | 주요 버튼, 점수 카드, 영상 플레이스홀더 |
| `--ams-color-white` | `#FFFFFF` | 페이지·카드 배경 |
| `--ams-color-surface` | `#F5F5F5` | 비활성 칩, 구분 배경 |
| `--ams-color-text` | `#111111` | 제목 |
| `--ams-color-text-secondary` | `#757575` | 부가 정보 |
| `--ams-color-border` | `#E0E0E0` | 카드·입력 테두리 |

다크 모드는 **v1 범위 외** — 목업은 라이트 전용.

## 타이포그래피

- **폰트**: Pretendard (없으면 `system-ui`, `Apple SD Gothic Neo`, `Malgun Gothic`, sans-serif)
- **제목**: Bold, 검정
- **본문/부가**: Regular, `--ams-color-text-secondary`

## 간격·형태

| 토큰 | 값 |
|------|-----|
| `--ams-radius-sm` | `8px` |
| `--ams-radius-md` | `12px` |
| `--ams-radius-lg` | `16px` |
| `--ams-radius-pill` | `9999px` |
| `--ams-space-page` | `16px` ~ `20px` |
| `--ams-border-width` | `1px` |

그림자는 **최소** — 플랫 또는 아주 약한 soft shadow만.

## 컴포넌트 패턴

### 하단 네비게이션

- 비활성: 회색 아웃라인 아이콘 + 회색 라벨
- 활성: `--ams-color-primary` 아이콘 + 라벨

### 과목·수강반 칩

- 선택: 검정 배경 + 흰 텍스트/아이콘
- 비선택: `--ams-color-surface` + 회색 텍스트

### 수강반 카드

- 선택: `--ams-color-surface` + 레드 상태 점
- 비선택: 흰 배경 + `--ams-color-border`

### 클리닉 예약 행

- 선택: `--ams-color-primary-light` + 레드 border + 시간 pill은 solid primary
- 잠금: 연한 레드 pill 배지

### 성실도 보고서

- 핵심 점수: **검정 카드** + 흰/레드 텍스트
- 보조 지표: 얇은 가로 progress bar (흰/레드)
- 강조 통계 카드: `--ams-color-primary-light` + 레드 숫자

### 진행바

- 트랙: `#E0E0E0`, 채움: `--ams-color-primary`, 끝단 `border-radius: pill`

## 구현 위치

- CSS 변수: `ams_frontend/src/styles/design-tokens.css`
- 신규 UI는 **하드코딩 hex 금지** — 토큰만 사용
- 기획 화면 매핑: [05-screens-by-role.md](../planning/05-screens-by-role.md)

## Phase 10 (디자인 전면 개편)

구현 슬라이스·화면 목록: [10-frontend-design.md](../planning/10-frontend-design.md).  
본 문서와 `reference/`가 **단일 기준**이며, Phase 10 완료 시 전 역할 화면이 여기에 맞춰진다.

## 변경 규칙

1. 목업과 충돌 시 → 목업·이 문서 우선, 변경은 PR/문서에 기록
2. 토큰 추가·변경 시 → `design-tokens.css`와 이 문서 동시 수정
3. 새 reference 이미지는 `reference/`에 번호 접두사로 추가
