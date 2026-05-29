# AMS 배포 (Docker + GitHub Actions)

## 1. NCP 서버 1회 설정

```bash
apt update && apt install -y docker.io docker-compose-v2
mkdir -p /opt/ams
```

`/opt/ams/.env` 생성 (레포 `.env.example` 참고):

```bash
MYSQL_ROOT_PASSWORD=...
AMS_DB_PASSWORD=...
AMS_JWT_SECRET=...   # 32자 이상
AMS_FRONTEND_BASE_URL=http://<공인IP 또는 도메인>
```

ACG: **80**(또는 `AMS_HTTP_PORT`), **22**(SSH). MySQL **3306 외부 개방 금지**.

DB 스키마는 **Flyway만** 사용 (`docs/db/FLYWAY.md`). 배포 후 `docker compose logs api | grep -i flyway` 로 마이그레이션 적용 여부 확인.

## 2. GitHub Secrets

| Secret | 설명 |
|--------|------|
| `SSH_HOST` | 서버 공인 IP |
| `SSH_USER` | `root` 등 |
| `SSH_PRIVATE_KEY` | 배포용 SSH 개인키 전체 |
| `GHCR_DEPLOY_TOKEN` | (private 패키지 시) `read:packages` PAT — 서버에서 `docker pull`용 |

공개 저장소 + public GHCR 패키지면 `GHCR_DEPLOY_TOKEN` 생략 가능.

## 3. CI/CD 동작

`main` push 시:

1. `ams_backend` — `./mvnw test`
2. `ams_frontend` — `npm ci` + `npm run build`
3. GHCR에 `ghcr.io/<owner>/ams/api`, `.../web` 이미지 push (소문자 경로)
4. SSH로 `/opt/ams`에 `docker-compose.yml` 동기화 후 `docker compose pull && up -d`

## 4. 로컬 Docker

```bash
cp .env.example .env
# .env 값 입력
docker compose up --build
```

http://localhost — API는 nginx가 `/api`로 프록시.

## 5. MySQL Workbench (SSH 터널)

운영 DB는 Docker `mysql`이며 **3306은 인터넷에 열지 않습니다.** `docker-compose.yml`이 MySQL을 **서버 `127.0.0.1:3306`** 에만 바인딩합니다.

### 서버에 compose 반영 (최초 1회 또는 배포 후)

```bash
cd /opt/ams
docker compose up -d
```

`ss -tlnp | grep 3306` → `127.0.0.1:3306` 만 보이면 OK.

### PC에서 터널 (PowerShell, 창 유지)

```powershell
ssh -i "C:\Users\mhm14\.ssh\ams-prod-key.pem" -L 3307:127.0.0.1:3306 root@<SSH_HOST>
```

- PC `localhost:3307` → 서버 `127.0.0.1:3306` → MySQL 컨테이너
- NCP ACG에 **3307/3306 추가 불필요**

### Workbench 연결

| 항목 | 값 |
|------|-----|
| Hostname | `127.0.0.1` |
| Port | `3307` |
| Username | `ams` (또는 `root`) |
| Password | `/opt/ams/.env`의 `AMS_DB_PASSWORD` / `MYSQL_ROOT_PASSWORD` |
| Default Schema | `ams` |

터널 SSH 세션이 끊기면 Workbench도 끊깁니다.
