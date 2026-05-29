-- Phase 2-6 보정: 동일 주·요일·시간이라도 조교가 다르면 별도 슬롯

ALTER TABLE clinic_slot DROP INDEX uk_clinic_slot;

ALTER TABLE clinic_slot
    ADD CONSTRAINT uk_clinic_slot UNIQUE (class_id, week_start_date, day_of_week, start_time, assistant_id);
