-- V{n}__short_description.sql  (Flyway가 실행하지 않음 — 예시만)
-- H2(test, MODE=MySQL) 호환: AFTER·UPDATE JOIN·RENAME INDEX 미사용

-- CREATE TABLE example (
--     id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
--     ...
--     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
-- );

-- ALTER TABLE some_table ADD COLUMN new_col VARCHAR(100) NULL;

-- FK가 UNIQUE를 쓰는 경우:
-- ALTER TABLE some_table DROP FOREIGN KEY fk_some;
-- ALTER TABLE some_table DROP INDEX uk_some;
-- ALTER TABLE some_table MODIFY ref_id BIGINT NULL;
-- ALTER TABLE some_table ADD CONSTRAINT fk_some FOREIGN KEY (ref_id) REFERENCES other (id);
