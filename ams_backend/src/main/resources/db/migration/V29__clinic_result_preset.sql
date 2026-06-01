-- Phase 12-7: clinic result preset · result_json
-- H2(test, MODE=MySQL) 호환: UPDATE JOIN·AFTER 미사용

CREATE TABLE clinic_result_preset (
    preset_id    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id     BIGINT       NOT NULL,
    name         VARCHAR(100) NOT NULL,
    field_schema JSON         NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_clinic_preset_class FOREIGN KEY (class_id) REFERENCES `class` (class_id),
    CONSTRAINT uk_clinic_preset_class_name UNIQUE (class_id, name)
);

INSERT INTO clinic_result_preset (class_id, name, field_schema)
SELECT c.class_id,
       '기본',
       JSON_OBJECT(
           'fields',
           JSON_ARRAY(
               JSON_OBJECT('key', 'attended', 'label', '참석', 'type', 'boolean', 'required', true),
               JSON_OBJECT('key', 'memo', 'label', '메모', 'type', 'text', 'maxLength', 500)
           )
       )
FROM `class` c;

ALTER TABLE clinic_slot
    ADD COLUMN clinic_result_preset_id BIGINT NULL;

UPDATE clinic_slot s
SET clinic_result_preset_id = (
    SELECT p.preset_id
    FROM clinic_result_preset p
    WHERE p.class_id = s.class_id AND p.name = '기본'
);

ALTER TABLE clinic_slot
    ADD CONSTRAINT fk_clinic_slot_preset
        FOREIGN KEY (clinic_result_preset_id) REFERENCES clinic_result_preset (preset_id);

ALTER TABLE clinic_slot
    MODIFY clinic_result_preset_id BIGINT NOT NULL;

ALTER TABLE clinic_reservation
    ADD COLUMN result_json JSON NULL;

ALTER TABLE clinic_reservation
    ADD COLUMN result_saved_at TIMESTAMP NULL;

UPDATE clinic_reservation
SET result_json = JSON_OBJECT('attended', result_attended, 'memo', result_memo),
    result_saved_at = CASE
        WHEN result_attended IS NOT NULL THEN updated_at
        ELSE NULL
    END
WHERE result_attended IS NOT NULL
   OR (result_memo IS NOT NULL AND result_memo <> '');
