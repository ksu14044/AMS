-- 회원가입 추가 수집항목: 전화번호, 개인정보 수집 동의 시각
ALTER TABLE `user`
    ADD COLUMN phone_number VARCHAR(20) NULL AFTER name,
    ADD COLUMN personal_info_consent_at TIMESTAMP NULL AFTER status;

-- 기존 사용자 데이터는 시스템 기준 시각으로 보정
UPDATE `user`
SET phone_number = COALESCE(phone_number, ''),
    personal_info_consent_at = COALESCE(personal_info_consent_at, created_at);

ALTER TABLE `user`
    MODIFY COLUMN phone_number VARCHAR(20) NOT NULL,
    MODIFY COLUMN personal_info_consent_at TIMESTAMP NOT NULL;
