-- 받는 통화 초기 데이터. 이미 있는 행은 덮어쓰지 않는다 (INSERT IGNORE)
-- decimal_places는 ISO 4217 표준값 기준이며 팀 확정 전 임시값이다
INSERT IGNORE INTO currency (currency_code, country_code, country_name, decimal_places, is_active)
VALUES ('IDR', 'ID', '인도네시아', 2, true),
       ('VND', 'VN', '베트남', 0, true),
       ('SGD', 'SG', '싱가포르', 2, true),
       ('KHR', 'KH', '캄보디아', 2, true);
