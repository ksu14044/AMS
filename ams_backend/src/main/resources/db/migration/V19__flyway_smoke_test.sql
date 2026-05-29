-- Flyway V19 동작 확인용 (앱 코드 미사용, nullable). 확인 후 V20에서 제거해도 됨.

ALTER TABLE academy ADD COLUMN flyway_smoke_verified_at TIMESTAMP NULL;
