# Flyway migrations (`db/migration`)

- **운영·로컬**: MySQL 8.0 (`ams`)
- **CI·단위 테스트**: H2 in-memory, `MODE=MySQL` (`application-test.yaml`)

두 환경에서 **같은 SQL 파일**이 적용되어야 한다.

## 새 마이그레이션 체크리스트

1. 파일명: `V{n}__snake_case_description.sql` (한 번 적용된 버전은 내용 변경 최소화)
2. 파일 첫 줄 근처: `-- H2(test, MODE=MySQL) 호환: ...` 주석
3. `_TEMPLATE.example.sql` 복사 후 작성
4. **반드시** `ams_backend`에서 `mvn test` 통과 확인 (`AmsApplicationTests`가 Flyway V1~최신까지 실행)
5. 로컬 MySQL에서 앱 기동·해당 기능 스모크 (선택이지만 권장)

## 자주 깨지는 문법 (H2)

| 사용 금지 | 권장 |
|-----------|------|
| `ADD COLUMN x ... AFTER y` | `ADD COLUMN x ...` (별도 `ALTER`로 나눠도 됨) |
| `UPDATE a JOIN b` | correlated subquery `UPDATE` |
| `RENAME INDEX` | `CREATE INDEX` + `DROP INDEX` |
| 한 `ALTER`에 컬럼 여러 `MODIFY` | `ALTER`당 한 컬럼 |

## FK / UNIQUE

MySQL은 FK가 UNIQUE 인덱스에 의존할 수 있다. 인덱스만 먼저 `DROP`하면 실패한다.

```sql
ALTER TABLE t DROP FOREIGN KEY fk_name;
ALTER TABLE t DROP INDEX uk_name;
-- 이후 컬럼/인덱스 변경
ALTER TABLE t ADD CONSTRAINT fk_name FOREIGN KEY (...) REFERENCES ...;
```

## 참고 마이그레이션

- `V15` — H2용 `MODIFY` 분리
- `V23` — `RENAME INDEX` 회피
- `V29` — `AFTER`·`UPDATE JOIN` 회피
- `V31` — FK 선삭제, `ADD COLUMN` without `AFTER`

## checksum mismatch (로컬만 실패, CI는 통과)

로그: `Migration checksum mismatch for migration version N`

**원인**: 로컬 MySQL에 **예전 내용**의 `VN`이 이미 적용됐는데, 저장소의 `VN__....sql`만 수정함. CI/H2는 빈 DB라 문제 없음.

**조치** (DB 스키마가 현재 파일과 맞을 때 — `mvn test` 통과 후 1회 repair):

```powershell
cd ams_backend
.\scripts\flyway-repair-local.ps1
```

(`application-local.yaml` 의 DB 설정 사용. PowerShell에서 URL의 `&`·비밀번호 `!` 를 `-D`에 넣으면 인자가 잘림.)

수동 실행 시 **작은따옴표** 필수:

```powershell
.\mvnw.cmd flyway:repair '-Dflyway.url=jdbc:mysql://localhost:3306/ams?serverTimezone=Asia/Seoul' '-Dflyway.user=root' '-Dflyway.password=YOUR_PASSWORD'
```

이후 `spring-boot:run`.

**주의**: 운영/공유 DB에는 repair 전 팀 합의. 앞으로는 배포된 `Vn`은 수정하지 말고 `V{n+1}` 추가.

## Cursor

에이전트 규칙: `.cursor/rules/flyway-migrations.mdc` (이 폴더의 `.sql` 편집 시 자동 참고)
