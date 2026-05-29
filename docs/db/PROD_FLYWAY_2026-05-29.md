# 운영 Flyway · 배포 파이프라인 정리 (2026-05-29)

thad.kr 운영 DB에 Flyway를 붙이고, **push → CI/CD → 자동 마이그레이션**까지 검증한 기록이다.

---

## 최종 상태 (검증 완료)

| 항목 | 내용 |
|------|------|
| 운영 DB | `flyway_schema_history` V1~V20, baseline 18 이후 19·20 스모크 적용·제거 완료 |
| 로컬 DB | 운영과 동일 (V19·V20 행 `success=1`, `flyway_smoke%` 컬럼 없음) |
| 배포 | `main` push → `publish-images` → `deploy` → `api` 기동 시 Flyway 자동 실행 |
| 서버 `/opt/ams/.env` | `AMS_API_IMAGE` / `AMS_WEB_IMAGE` = `ghcr.io/ksu14044/ams/api|web:latest` |
| 도메인 | http://thad.kr |

**성공 판정 SQL (로컬·운영 동일하면 OK):**

```sql
SELECT version, description, success
FROM flyway_schema_history
WHERE version IN ('19', '20');

SHOW COLUMNS FROM academy LIKE 'flyway_smoke%';
```

기대:

- 19 `flyway smoke test` — success `1`
- 20 `drop flyway smoke test` — success `1`
- `flyway_smoke%` — **0행**

---

## 정상 운영 흐름 (앞으로)

1. `ams_backend/src/main/resources/db/migration/V21__설명.sql` 추가
2. 로컬 `./mvnw spring-boot:run` 으로 적용·기능 확인
3. `main` push
4. GitHub Actions: `test-backend` → `build-frontend` → `publish-images` → `deploy` 성공
5. 서버 `api` 재기동 시 Flyway가 **미적용 버전만** 자동 실행

**수동으로 할 일 없음** (`SPRING_FLYWAY_TARGET`, `compose run` 등은 장애·1회 복구 때만).

### deploy가 이미지를 고르는 방식

`/.github/workflows/ci-cd.yml` 의 deploy job은 서버에서:

```bash
source .env
export AMS_API_IMAGE AMS_WEB_IMAGE   # ← 이 push의 github.sha 로 덮어씀
docker compose pull && docker compose up -d
```

- **CI deploy:** 항상 **그 push 커밋 SHA** 태그 이미지 (`ghcr.io/.../api:<40자 SHA>`)
- **서버에서 수동 `docker compose run`:** `.env`의 `AMS_API_IMAGE` 를 그대로 사용

`.env`에 옛 SHA가 남아 있어도 **GitHub deploy는 SHA 이미지**를 쓴다.  
다만 **수동 작업·pull 테스트**는 `.env` 기준이므로 `latest` 또는 **실제 pull 되는 태그**를 맞춰 둔다.

### 운영 Flyway 로그 확인

```bash
cd /opt/ams
docker compose logs api 2>&1 | grep -E "Flyway|Migrating|Current version|Successfully applied"
```

---

## 1회 작업 이력 (당시 DB·이미지 상태)

### 초기 운영 DB

- 로컬 덤프(`Dump20260529.sql`)로 Workbench import → 스키마 V1~V18 수준
- `flyway_schema_history` 가 덤프에 포함되어 있었고, **V2 failed** 등 깨진 이력이 있었음

### baseline 18 (1회)

1. Workbench/터널: `DROP TABLE flyway_schema_history;`
2. `docker compose run --rm -e SPRING_FLYWAY_BASELINE_VERSION=18 api`  
   → `Successfully baselined schema with version: 18`
3. `docker compose up -d api`  
   → 이력에 **version 18, BASELINE, success=1** 만 남김
4. `.env`에 `SPRING_FLYWAY_BASELINE_VERSION` **넣지 않음** (상시 해제)

### V19가 자동 적용되지 않았던 이유

| 원인 | 설명 |
|------|------|
| `.env` 고정 SHA `13b4e24...` | 해당 이미지 **jar에 V19 SQL 없음** (4단계 `unzip \| grep V19` 빈 결과) |
| Actions SHA `189110e...` | GHCR `docker pull` 시 **manifest unknown** (태그 미존재) |
| 수동 `compose run` | `.env` 이미지 사용 → 옛 이미지로 기동, DB는 18에서 “up to date” |

### V19 수동 적용 (1회)

1. `docker pull ghcr.io/ksu14044/ams/api:latest` — **V19 포함** 확인
2. `.env` → `api:latest`, `web:latest`
3. `docker compose run --rm -e SPRING_FLYWAY_TARGET=19 api`  
   → `Migrating ... version "19"` / `now at version v19`
4. `docker compose up -d api`

### V20 배포 파이프라인 재검증

- 파일: `V20__drop_flyway_smoke_test.sql` — 스모크 컬럼 DROP
- `main` push → deploy 성공 → **추가 수동 작업 없이** V20 적용
- 로컬·운영 SQL 결과 동일 → **push만으로 Flyway 동작 확인 완료**

---

## 이미지·jar 확인 (JRE 이미지에 `jar` 없음)

```bash
docker create --name ams-jar-check ghcr.io/ksu14044/ams/api:latest
docker cp ams-jar-check:/app/app.jar /tmp/ams-app.jar
docker rm ams-jar-check
unzip -l /tmp/ams-app.jar | grep db/migration/V19
```

---

## 스모크 마이그레이션 파일 (이력만 남음)

| 파일 | 용도 |
|------|------|
| `V19__flyway_smoke_test.sql` | `academy.flyway_smoke_verified_at` 추가 (앱 미사용) |
| `V20__drop_flyway_smoke_test.sql` | 위 컬럼 제거 |

운영 DB에는 **컬럼 없음**, 이력 19·20만 남는다. 삭제하지 않는다 (이미 적용된 migration).

---

## 트러블슈팅 요약

| 증상 | 조치 |
|------|------|
| `manifest unknown` (특정 SHA pull 실패) | GHCR Packages에 해당 태그 있는지 확인. 없으면 **성공한 publish-images 커밋 SHA** 또는 `latest` 사용 |
| `Schema is up to date` 인데 V19 미적용 | 실행 중 이미지 jar에 migration 있는지 `unzip` 확인. `.env` / 수동 run 이미지 갱신 |
| `Detected failed migration to version 2` | `DROP flyway_schema_history` 후 baseline 18 재실행 (`DEPLOY.md` §6) |
| `compose run` 이 `ams_backend` 빌드 시도 | `/opt/ams/.env`에 `AMS_API_IMAGE` 미설정 또는 주석 처리 |
| `jar: not found` | 정상(JRE). `unzip -l /tmp/ams-app.jar` 사용 |

---

## 관련 문서

- [FLYWAY.md](./FLYWAY.md) — 일상 원칙·새 migration 절차
- [../deploy/DEPLOY.md](../deploy/DEPLOY.md) — 서버·SSH 터널·baseline 1회 절차
