-- 집계 대상 0건일 때 달성률·등급 N/A (NULL)

ALTER TABLE diligence_report
    MODIFY homework_rate INT NULL,
    MODIFY clinic_rate INT NULL,
    MODIFY video_rate INT NULL,
    MODIFY homework_grade CHAR(1) NULL,
    MODIFY clinic_grade CHAR(1) NULL,
    MODIFY video_grade CHAR(1) NULL;
