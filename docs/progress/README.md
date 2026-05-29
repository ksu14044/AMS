# 일일 진행 관리

**오늘 무엇을 했는지**, **내일 무엇을 할지**를 1~2분 안에 파악하기 위한 문서 체계.

## 문서 4종 (역할 분리)

| 문서 | 갱신 주기 | 역할 |
|------|-----------|------|
| **[STATUS.md](./STATUS.md)** | 작업 시작·종료 시 | **지금** 어디인지 — Phase, 이번 주 목표, 다음 할 일 (코드와 불일치 시 **코드 우선**으로 갱신) |
| **[PHASE-SCOPE.md](./PHASE-SCOPE.md)** | Phase·슬라이스 변경 시 | **완료/제외/슬라이스** 정의 (ROADMAP과 코드 불일치 방지) |
| **[daily/](./daily/)** | **하루 1회** (퇴근 전) | **그날** 한 일·메모·결정 |
| [ROADMAP.md](../planning/ROADMAP.md) | 체크리스트 완료 시 | **전체** 단계별 할 일 목록 (Phase 0~11) |

```
ROADMAP (장기 할 일)
    ↑ 체크 완료 시 반영
PHASE-SCOPE (Phase별 완료·제외·슬라이스) ←── Phase 경계 확인
    ↑
STATUS (현재 스냅샷) ←── 매일 읽기
    ↑ 퇴근 전 요약
daily/YYYY-MM-DD.md (일지)
```

## 매일 루틴 (권장 5분)

### 아침 — 계획 (2분)

1. [STATUS.md](./STATUS.md) 열기 → **현재 Phase**, **오늘 할 일**, **블로커** 확인
2. [PHASE-SCOPE.md](./PHASE-SCOPE.md) → 이번 작업이 **어느 슬라이스**인지 확인
3. 어제 일지: `daily/`에서 가장 최근 파일 → **미완료(In progress)** 항목 이어가기
4. [ROADMAP.md](../planning/ROADMAP.md) 해당 Phase 섹션에서 오늘 맞출 체크박스 1~3개 고르기

### 저녁 — 기록 (3분)

1. `daily/YYYY-MM-DD.md` 작성 ([템플릿](./daily/_template.md) 복사)
2. [STATUS.md](./STATUS.md) 갱신
3. Phase·슬라이스 완료 시 [PHASE-SCOPE.md](./PHASE-SCOPE.md) 표 갱신
4. ROADMAP에서 **완료한 항목** `[ ]` → `[x]`

## 파일 규칙

- 일지 파일명: `daily/YYYY-MM-DD.md` (예: `2026-05-20.md`)
- 하루에 여러 번 작업해도 **일지는 하루 1파일** — 추가 작업은 같은 파일에 섹션追加
- `STATUS.md`만 보면 되게 **일지에 장문 스펙 쓰지 않기** — 결정·이슈는 [DECISIONS.md](../planning/DECISIONS.md)로

## STATUS.md 필드 설명

| 섹션 | 내용 |
|------|------|
| 현재 Phase | ROADMAP Phase 번호 + 한 줄 목표 |
| 이번 주 목표 | 월~일 기준 3개 이내 |
| 다음 할 일 | 우선순위 순 (Phase 10 디자인 → Phase 11 운영) |
