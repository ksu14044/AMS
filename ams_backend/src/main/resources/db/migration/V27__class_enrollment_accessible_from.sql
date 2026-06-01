-- Phase 12-5: 학생 조회 시작일(accessible_from)

ALTER TABLE class_enrollment
    ADD COLUMN accessible_from DATE NULL;
