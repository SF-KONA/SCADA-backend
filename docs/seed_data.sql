-- =====================================================
-- SCADA 공정 최적화 시연용 시드 데이터
-- =====================================================
-- 사용법:
--   mysql -u root -p scada < seed_data.sql
-- =====================================================

USE scada;

-- 0. 기존 데이터 정리 (재실행 안전성)
DELETE FROM equipment_measurements;
DELETE FROM oee_metrics;
DELETE FROM equipment_parameters;
DELETE FROM equipments;
DELETE FROM processes WHERE step_no = '02';

-- =====================================================
-- 1. processes (공정 마스터)
-- =====================================================
INSERT INTO processes (step_no, process_name, has_equipment, sort_order, equipment_type, equipment_code)
VALUES ('02', '산화', true, 2, 'Vertical Diffusion Furnace', 'FURN');

-- =====================================================
-- 2. equipments (설비 마스터)
-- =====================================================
INSERT INTO equipments (equipment_id, equipment_name, step_no, total_running_hours, unit_no) VALUES
('FURN_01', '수직형 확산로 #1', '02', 4210, 1),
('FURN_02', '수직형 확산로 #2', '02', 4180, 2),
('FURN_03', '수직형 확산로 #3', '02', 4102, 3);

-- =====================================================
-- 3. equipment_parameters (제어 대상 파라미터)
-- 각 설비당 3개: 온도, 압력, O2 유량
-- 모두 is_controllable=true (제어 가능)
-- =====================================================
INSERT INTO equipment_parameters
  (equipment_id, tag_code, tag_name, unit, normal_min, normal_max,
   data_type, param_category, is_controllable, collection_period)
VALUES
  -- FURN_01
  ('FURN_01', 'FURN_01_TEMP',     '노내 온도',    '°C',  900.0, 1100.0, 'FLOAT', 'PROCESS', true, '5s'),
  ('FURN_01', 'FURN_01_PRESSURE', '챔버 압력',    'bar', 15.0,  20.0,   'FLOAT', 'PROCESS', true, '5s'),
  ('FURN_01', 'FURN_01_O2_FLOW',  'O2 가스 유량', 'slm', 4.0,   5.0,    'FLOAT', 'PROCESS', true, '5s'),
  -- FURN_02
  ('FURN_02', 'FURN_02_TEMP',     '노내 온도',    '°C',  900.0, 1100.0, 'FLOAT', 'PROCESS', true, '5s'),
  ('FURN_02', 'FURN_02_PRESSURE', '챔버 압력',    'bar', 15.0,  20.0,   'FLOAT', 'PROCESS', true, '5s'),
  ('FURN_02', 'FURN_02_O2_FLOW',  'O2 가스 유량', 'slm', 4.0,   5.0,    'FLOAT', 'PROCESS', true, '5s'),
  -- FURN_03
  ('FURN_03', 'FURN_03_TEMP',     '노내 온도',    '°C',  900.0, 1100.0, 'FLOAT', 'PROCESS', true, '5s'),
  ('FURN_03', 'FURN_03_PRESSURE', '챔버 압력',    'bar', 15.0,  20.0,   'FLOAT', 'PROCESS', true, '5s'),
  ('FURN_03', 'FURN_03_O2_FLOW',  'O2 가스 유량', 'slm', 4.0,   5.0,    'FLOAT', 'PROCESS', true, '5s');

-- =====================================================
-- 4. equipment_measurements (최신 측정값)
--
-- 시나리오:
--   FURN_01 → 모두 정상 (제안 안 만들어져야 함)
--   FURN_02 → 모두 이탈 (3개 모두 제안 생성)
--   FURN_03 → 온도만 살짝 이탈 (1개 제안)
-- =====================================================
INSERT INTO equipment_measurements (param_id, measured_value, measured_at) VALUES
  -- FURN_01 (정상)
  ((SELECT param_id FROM equipment_parameters WHERE tag_code='FURN_01_TEMP'),     1050.0, NOW(3)),
  ((SELECT param_id FROM equipment_parameters WHERE tag_code='FURN_01_PRESSURE'), 18.0,   NOW(3)),
  ((SELECT param_id FROM equipment_parameters WHERE tag_code='FURN_01_O2_FLOW'),  4.5,    NOW(3)),
  -- FURN_02 (전부 이탈)
  ((SELECT param_id FROM equipment_parameters WHERE tag_code='FURN_02_TEMP'),     1180.0, NOW(3)),
  ((SELECT param_id FROM equipment_parameters WHERE tag_code='FURN_02_PRESSURE'), 22.0,   NOW(3)),
  ((SELECT param_id FROM equipment_parameters WHERE tag_code='FURN_02_O2_FLOW'),  6.5,    NOW(3)),
  -- FURN_03 (온도만 살짝 이탈)
  ((SELECT param_id FROM equipment_parameters WHERE tag_code='FURN_03_TEMP'),     1110.0, NOW(3)),
  ((SELECT param_id FROM equipment_parameters WHERE tag_code='FURN_03_PRESSURE'), 17.0,   NOW(3)),
  ((SELECT param_id FROM equipment_parameters WHERE tag_code='FURN_03_O2_FLOW'),  4.5,    NOW(3));

-- =====================================================
-- 5. oee_metrics (현재 OEE)
-- predictedOee 계산용 베이스라인
-- =====================================================
INSERT INTO oee_metrics
  (equipment_id, period_start, availability, performance, quality, oee, computed_at)
VALUES
  ('FURN_01', NOW(), 92.0, 95.0, 96.0, 83.9, NOW()),  -- 정상 가동
  ('FURN_02', NOW(), 85.0, 78.0, 82.0, 54.4, NOW()),  -- 저하
  ('FURN_03', NOW(), 88.0, 87.0, 90.0, 68.9, NOW());  -- 중간

-- =====================================================
-- 검증
-- =====================================================
SELECT '=== 설비 ===' AS info;
SELECT * FROM equipments;

SELECT '=== 파라미터 현황 (이탈 표시) ===' AS info;
SELECT
  ep.tag_code,
  ep.tag_name,
  ROUND(em.measured_value, 1) AS current_value,
  CONCAT(ep.normal_min, ' ~ ', ep.normal_max) AS normal_range,
  CASE
    WHEN em.measured_value > ep.normal_max OR em.measured_value < ep.normal_min
    THEN '🔴 이탈'
    ELSE '🟢 정상'
  END AS status
FROM equipment_parameters ep
JOIN equipment_measurements em ON em.param_id = ep.param_id
ORDER BY ep.equipment_id, ep.tag_code;
