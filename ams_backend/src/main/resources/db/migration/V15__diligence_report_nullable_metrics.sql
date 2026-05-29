-- 집계 대상 0건일 때 달성률·등급 N/A (NULL)
-- H2(테스트)는 한 ALTER에 MODIFY 여러 개 불가 → 컬럼별 실행

ALTER TABLE diligence_report MODIFY homework_rate INT NULL;
ALTER TABLE diligence_report MODIFY clinic_rate INT NULL;
ALTER TABLE diligence_report MODIFY video_rate INT NULL;
ALTER TABLE diligence_report MODIFY homework_grade CHAR(1) NULL;
ALTER TABLE diligence_report MODIFY clinic_grade CHAR(1) NULL;
ALTER TABLE diligence_report MODIFY video_grade CHAR(1) NULL;
