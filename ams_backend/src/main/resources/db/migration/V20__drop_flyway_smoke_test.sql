-- V19 Flyway 배포 파이프라인 검증용 컬럼 제거

ALTER TABLE academy DROP COLUMN flyway_smoke_verified_at;
