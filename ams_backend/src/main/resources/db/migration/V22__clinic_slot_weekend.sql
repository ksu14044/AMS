-- 클리닉 슬롯 요일: 토·일 포함

ALTER TABLE clinic_slot DROP CONSTRAINT chk_clinic_day;

ALTER TABLE clinic_slot ADD CONSTRAINT chk_clinic_day
    CHECK (day_of_week IN ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'));
