# 10. 기술 및 구현 고려사항

| 영역 | 요구사항 |
|------|----------|
| 인증·접근 | RBAC, 학원 단위 테넌트(`academy_id`), 학생 타 반 API 차단, RLS 권장 |
| 영상 | YouTube oEmbed/Data API, 비공개 오류 처리 |
| 이미지 | 10MB, JPEG/PNG, 스토리지 경로 **학생 ID + 날짜** 분리 (원본). 구현 시 `academy_id` 접두 권장 |
| 클리닉 | Cron 토 23:00 잠금, 월 00:00 슬롯 생성, `Asia/Seoul` |
| 알림 | FCM/APNS, D-1 스케줄러 |
| 보고서 | 테스트 입력 이벤트 → 집계 → PDF |
| 데이터 | 활동 로그 영구 보존, 반 삭제 시 아카이브 |
| 오프라인 | 반 콘텐츠 캐싱, 클리닉 신청은 온라인 필수 |

## 현재 AMS 스택 매핑

| 기획 | 구현 |
|------|------|
| API 서버 | `ams_backend` Spring Boot 4 |
| DB | MySQL 8 `ams` |
| 웹 클라이언트 | `ams_frontend` React + Vite |
| 마이그레이션 | Flyway 추가 권장 (Phase 0) |
| Security | Spring Security + JWT (OAuth 의존성은 필요 시 정리) |

## 회원가입 플로우

**교직원:** 이메일·비밀번호 → 역할(7종) 선택 → 학원 코드 → 원장 승인 시 역할·과목 **확정/수정** → 활성화 ([DECISIONS.md](./DECISIONS.md) §1)

**학생:** 이름·이메일·비밀번호 → 학원 코드 → 배정 대기 → 배정 알림 → 반 열람

## 보안 체크리스트 (Phase 11 — 운영)

> 갱신 2026-05-26 — 구현 여부는 `ams_backend` 소스 기준.

- [ ] HTTPS (배포·인프라)
- [x] 비밀번호 BCrypt (`BCryptPasswordEncoder`, Phase 0~)
- [ ] 파일 MIME 검증 — **magic byte** (Phase 4: `Content-Type`·용량만)
- [x] IDOR 방지 (`ClassAccessService` 등, Phase 2~)
- [ ] Rate limiting (로그인)
- [x] CORS (`SecurityConfig`, 기반)
