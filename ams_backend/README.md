# ams_backend

Spring Boot API 서버 (Phase 0: 인증·테넌트).

## 로컬 DB 설정

1. `src/main/resources/application-local.yaml.example` 를 복사해 `application-local.yaml` 생성
2. MySQL `password`, `ams.jwt.secret`(32자 이상) 입력 (`application-local.yaml` 은 git에 올라가지 않음)
3. MySQL에 `ams` 데이터베이스 생성 후 애플리케이션 실행

```bash
./mvnw spring-boot:run
```

기본 프로필: `local` · Flyway가 `db/migration/V1__init_academy_user.sql` 적용

### DB 마이그레이션 추가 시

- 가이드: [`src/main/resources/db/migration/README.md`](src/main/resources/db/migration/README.md)
- **CI는 H2**로 Flyway 전체를 돌린다 → `mvn test` 통과 필수 (`AFTER`, `UPDATE JOIN` 등 MySQL 전용 문법 금지)
- 예시 템플릿: `db/migration/_TEMPLATE.example.sql`

## API (Phase 0)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/auth/signup/academy` | 학원 개설 + 원장 |
| POST | `/api/v1/auth/signup/staff` | 교직원 가입 (PENDING) |
| POST | `/api/v1/auth/signup/student` | 학생 가입 |
| POST | `/api/v1/auth/login` | 로그인 (이메일·비밀번호) |
| POST | `/api/v1/auth/login/select` | 다중 소속 시 소속 선택 후 토큰 발급 |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 |
| GET | `/api/v1/admin/users/pending` | 승인 대기 교직원 목록 |
| POST | `/api/v1/admin/users/{id}/approve` | 원장 승인 |
| GET | `/api/v1/me` | 내 정보 |

Swagger: http://localhost:8080/swagger-ui.html
