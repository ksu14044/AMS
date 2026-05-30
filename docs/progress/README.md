# 일일 진행 관리

**오늘 무엇을 했는지**, **내일 무엇을 할지**를 1~2분 안에 파악하기 위한 문서 체계.

## 문서 4종 (역할 분리)

| 문서 | 갱신 주기 | 역할 |
|------|-----------|------|
| **[STATUS.md](./STATUS.md)** | 작업 시작·종료 시 | **지금** 어디인지 — Phase, 다음 할 일 |
| **[PHASE-SCOPE.md](./PHASE-SCOPE.md)** | Phase·슬라이스 변경 시 | **완료/제외/슬라이스** 정의 |
| **[daily/](./daily/)** | **하루 1회** | **그날** 한 일·메모 |
| [ROADMAP.md](../planning/ROADMAP.md) | 체크리스트 완료 시 | Phase 0~10 · **12 v3.0** · **11 운영(최종)** |

```
ROADMAP (장기)
    ↑
PHASE-SCOPE (Phase 12 · 11 경계)
    ↑
STATUS (스냅샷)
    ↑
daily/ (일지)
```

## Phase 실행 순서 (2026-05-30)

| 순서 | Phase | 비고 |
|------|-------|------|
| ✅ | 0~10 | v2.0 완료 |
| 🔲 | 10.2 | BE 카드 메타 (선택) |
| 🔲 | **12** | **v3.0 — 다음** |
| 🔲 | **11** | **운영 — 맨 마지막** |

## 매일 루틴 (권장 5분)

### 아침

1. [STATUS.md](./STATUS.md) — 현재 Phase · **다음: 12-1** · 11은 최종
2. [PHASE-SCOPE.md](./PHASE-SCOPE.md) — 슬라이스 번호 확인
3. 어제 `daily/` → 미완료 이어가기

### 저녁

1. `daily/YYYY-MM-DD.md` 작성
2. STATUS · PHASE-SCOPE · ROADMAP 갱신

## STATUS.md 필드

| 섹션 | 내용 |
|------|------|
| 다음 Phase | **12 v3.0** |
| 최종 Phase | **11 운영** (12 완료 후) |
| v3.0 기획 | [DECISIONS §10~§19](../planning/DECISIONS.md) |

## 파일 규칙

- 일지: `daily/YYYY-MM-DD.md` 하루 1파일
- 스펙·결정: [planning/](../planning/) — 일지에 장문 금지

## v3.0 기획 (2026-05-30)

기획 문서 반영 완료. 구현 착수 = **Phase 12-1**.

- [01-service-overview](../planning/01-service-overview.md)
- [DECISIONS §10~§19](../planning/DECISIONS.md)
- [ROADMAP Phase 12](../planning/ROADMAP.md)
