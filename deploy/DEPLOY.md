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
3. GHCR에 `api`, `web` 이미지 push
4. SSH로 `/opt/ams`에 `docker-compose.yml` 동기화 후 `docker compose pull && up -d`

## 4. 로컬 Docker

```bash
cp .env.example .env
# .env 값 입력
docker compose up --build
```

http://localhost — API는 nginx가 `/api`로 프록시.
