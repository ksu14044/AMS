# 04. 반(Class) 데이터 구조

각 반은 **8개 섹션**. 공지·일정·교재는 **수업기록과 별도**.

| 섹션 | 주요 필드 | 편집 | 비고 |
|------|-----------|------|------|
| **수업기록** | 수업일, 요약, 귀속 항목 | 선생·조교 | v3.0 집계 단위 ([§10](./DECISIONS.md#10-수업기록-lesson-record)) |
| 수업 정보 | 요일, 시간, 강의실 | 담임 | 별도 |
| 공지 | 제목, 내용, 첨부 | 선생·조교 | 별도 |
| 교재 | 제목, 출판사, 진도 | 담임 | 별도 |
| 영상 | URL, **대상 학생** | 선생·조교 | 수업기록 귀속; 기본 미선택 |
| 숙제 | 문항 수, 정오표, 점수 | 선생·조교 | 마감 없음; 점수=완료 |
| 테스트 | 문항 수, 재시험 기준, **석차** | 선생·조교 | 재시험 최대 3회 |
| 클리닉 | 시작 시각, 정원, **프리셋** | 선생·조교 | 정원 기본 10 |

## 수업기록

- `lesson_date` + `summary` 1건 = 1회 수업
- 숙제·테스트·클리닉·영상 → `lesson_record_id`
- `class_enrollment.accessible_from` — 조회 시작일 ([§15](./DECISIONS.md#15-학생-조회-시작일))

## 숙제·테스트 채점

- 정답지·학생 답안 **수동**, **교직원 only** ([§11](./DECISIONS.md#11-정오표채점-수동))
- `raw_score = ceil(100 ÷ question_count × correct_count, 소수 첫째 자리)`

## 테스트 석차·재시험

- UI: **석차** ([§13](./DECISIONS.md#13-테스트-석차--게이지))
- 재시험: 기준 문항 수, 올림/내림 판정, 최대 3회, 최종=마지막 ([§12](./DECISIONS.md#12-재시험))

## 엔티티 관계

```
Class 1──* LessonRecord 1──* [Homework, Test, Video, ClinicSlot]
     1──* [Notice, Schedule, Textbook]   ← 별도
     *──* Student (enrollment + accessible_from)
     *──* Parent (parent_student_link)
```

## 상태값

- 숙제/테스트: `OPEN` | `COMPLETED`
- 알림: `ACTIVE` | `DISMISSED` ([§16](./DECISIONS.md#16-알림-완료-기준))

[database/schema-overview.md](./database/schema-overview.md)
