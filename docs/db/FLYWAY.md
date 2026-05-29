# Flyway (DB 스키마)

## 원칙

- **스키마 변경은 `ams_backend/src/main/resources/db/migration/`의 SQL 파일만** 사용한다.
- Workbench에서 `CREATE TABLE` / `ALTER` 로 구조를 바꾸지 않는다.
- 운영 DB를 로컬 덤프로 덮어쓰지 않는다. 배포 시 **api 기동 → Flyway 자동 적용**.

## Spring Boot 4 필수 의존성

`flyway-core`만 있으면 **기동 시 Flyway가 실행되지 않습니다.** `pom.xml`에 다음이 있어야 합니다.

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
```

기동 로그에 `Flyway` / `Successfully applied` / `Schema is up to date` 가 보여야 정상입니다.

## Flyway가 도는 시점

| 환경 | 언제 |
|------|------|
| 로컬 | `./mvnw spring-boot:run` (profile `local`) |
| 테스트 | `./mvnw test` |
| 운영 | Docker `api` 컨테이너 기동 시 (`SPRING_PROFILES_ACTIVE=prod`) |

Workbench로 MySQL에만 접속해도 Flyway는 **실행되지 않는다**.  
예전에 “Flyway가 안 돈다”고 느낀 경우는, **앱을 띄우지 않고 SQL만 직접 실행**했을 가능성이 크다.

## 새 마이그레이션 추가 절차

1. 다음 번호 파일 추가: `V21__설명.sql` (현재 최신 적용: **V20**)
2. 로컬에서 백엔드 기동 → 적용·기능 확인
3. `main` push → CI/CD 배포 → 서버 `api` 재기동 시 운영 DB에 **자동** 반영

**운영 검증 기록 (2026-05-29, V19/V20 스모크):** [PROD_FLYWAY_2026-05-29.md](./PROD_FLYWAY_2026-05-29.md)

### 배포 후 확인 (로컬·운영 동일하면 OK)

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;
```

## 로그 확인

**로컬** — 콘솔에 `Flyway` / `Successfully applied` / `Schema is up to date`  
**운영**

```bash
cd /opt/ams
docker compose logs api 2>&1 | grep -i flyway
```

## 이미 Workbench로 V1~V18을 맞춘 DB (1회 정리)

덤프에 `flyway_schema_history`가 **포함되어 있고** 버전이 V18까지면 추가 작업 없음.

테이블은 있는데 `flyway_schema_history`가 **없거나 비어 있으면**, api 기동 시 V2부터 다시 돌려 **“already exists”** 오류가 날 수 있다.

**운영·로컬 공통 (SSH 터널 또는 로컬 Workbench):**

```sql
DROP TABLE IF EXISTS flyway_schema_history;
```

이후 앱 1회 기동 (`SPRING_FLYWAY_BASELINE_VERSION=18`).  
빈 테이블만 두면 baseline이 안 될 수 있습니다.

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

| 상태 | 조치 |
|------|------|
| V18까지 행이 있고 `success=1` | 추가 작업 없음. 이후 migration 파일만 추가 후 **push → deploy** |
| 테이블은 있는데 이력 없음 | Workbench로 V1~V18을 이미 실행한 상태. **api를 띄우기 전**에 스키마가 최신 migration과 동일한지 확인 후, Flyway `baseline`(버전 18) 또는 로컬에서 이력이 포함된 덤프로 `flyway_schema_history` 맞추기. 잘못 baseline 하면 이후 마이그레이션이 안 돌거나 중복 실행됨 |
| 빈 DB (테이블 없음) | 덤프 import 없이 `api`만 기동 → V1~V18 자동 적용 |

**앞으로는** 수동 SQL 대신 **반드시 migration 파일 + api 기동**만 사용한다.

## 설정 (`application.yaml`)

- `flyway.enabled: true`
- `validate-on-migrate: true` — 파일 변경 시 기동 실패 (수동 DB와 코드 불일치 방지)
- `clean-disabled: true` — `flyway clean`으로 DB 전체 삭제 방지
