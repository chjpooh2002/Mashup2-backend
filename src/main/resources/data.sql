-- 받는 통화 초기 데이터. 이미 있는 행은 덮어쓰지 않는다 (INSERT IGNORE)
-- decimal_places는 ISO 4217 표준값 기준이며 팀 확정 전 임시값이다
INSERT IGNORE INTO currency (currency_code, country_code, country_name, decimal_places, is_active)
VALUES ('IDR', 'ID', '인도네시아', 2, true),
       ('VND', 'VN', '베트남', 0, true),
       ('SGD', 'SG', '싱가포르', 2, true),
       ('KHR', 'KH', '캄보디아', 2, true);

-- 미지원 수단은 목록에 노출해야 하므로 is_active = true로 넣고 선택만 unselectable_reason으로 막는다.
-- is_active = false로 넣으면 ReceiveMethodRepository가 조회에서 제외해 선택 불가 사유가 응답에 나오지 않는다
INSERT IGNORE INTO receive_method (receive_currency, method_type, method_provider, method_name, speed_type,
                                   fixed_fee, rate_margin, min_amount, max_amount, unselectable_reason, is_active)
VALUES
-- 싱가포르
('SGD', 'BANK', 'NONE', '은행', 'EXPRESS', 5000, 0.015, 10000.0000, 5000000.0000, NULL, true),
('SGD', 'BANK', 'NONE', '은행', 'STANDARD', 2500, 0.008, 10000.0000, 5000000.0000, NULL, true),
('SGD', 'CASH_PICKUP', 'MONEY_GRAM', '머니그램 캐쉬픽업', 'EXPRESS', 5000, 0.020, 10000.0000, 1500000.0000, NULL, true),
('SGD', 'CARD', 'VISA', 'Visa카드', 'EXPRESS', 5000, 0.025, 10000.0000, 2000000.0000, NULL, true),
('SGD', 'PAYNOW', 'NONE', 'PAYNOW', 'EXPRESS', 5000, 0.012, 10000.0000, 3000000.0000, '지원하지 않습니다.', true),
('SGD', 'PAYNOW', 'NONE', 'PAYNOW', 'STANDARD', 2500, 0.006, 10000.0000, 3000000.0000, '지원하지 않습니다.', true),

-- 인도네시아
('IDR', 'BANK', 'NONE', '은행', 'EXPRESS', 5000, 0.015, 10000.0000, 5000000.0000, NULL, true),
('IDR', 'BANK', 'NONE', '은행', 'STANDARD', 2500, 0.008, 10000.0000, 5000000.0000, NULL, true),
('IDR', 'CASH_PICKUP', 'MONEY_GRAM', '머니그램 캐쉬픽업', 'EXPRESS', 5000, 0.020, 10000.0000, 1500000.0000, NULL, true),
('IDR', 'CARD', 'VISA', 'Visa카드', 'EXPRESS', 5000, 0.025, 10000.0000, 2000000.0000, NULL, true),
('IDR', 'MOBILE_WALLET', 'GOPAY', 'GoPay모바일 월렛', 'EXPRESS', 5000, 0.013, 10000.0000, 1000000.0000, NULL, true),
('IDR', 'MOBILE_WALLET', 'OVO', 'OVO 모바일 월렛', 'EXPRESS', 5000, 0.014, 10000.0000, 1000000.0000, NULL, true),
('IDR', 'MOBILE_WALLET', 'GENERAL', '모바일 월렛', 'EXPRESS', 5000, 0.017, 10000.0000, 1000000.0000, NULL, true),

-- 베트남
('VND', 'BANK', 'NONE', '은행', 'EXPRESS', 5000, 0.015, 10000.0000, 5000000.0000, NULL, true),
('VND', 'BANK', 'NONE', '은행', 'STANDARD', 2500, 0.008, 10000.0000, 5000000.0000, NULL, true),
('VND', 'CARD', 'ATM', 'ATM카드', 'EXPRESS', 5000, 0.016, 10000.0000, 2000000.0000, NULL, true),
('VND', 'CARD', 'ATM', 'ATM카드', 'STANDARD', 2500, 0.009, 10000.0000, 2000000.0000, NULL, true),
('VND', 'MOBILE_WALLET', 'GENERAL', '모바일 월렛', 'EXPRESS', 5000, 0.017, 10000.0000, 1000000.0000, NULL, true),
('VND', 'CASH_PICKUP', 'MONEY_GRAM', '머니그램 캐쉬픽업', 'EXPRESS', 5000, 0.020, 10000.0000, 1500000.0000, NULL, true),
('VND', 'CASH_PICKUP', 'VIET_COM_BANK', 'Vietcombank캐쉬픽업', 'EXPRESS', 7000, 0.018, 10000.0000, 1500000.0000, NULL, true),
('VND', 'CARD', 'VISA', 'Visa카드', 'EXPRESS', 5000, 0.025, 10000.0000, 2000000.0000, '지원하지 않습니다.', true),

-- 캄보디아
('KHR', 'BANK', 'NONE', '은행', 'EXPRESS', 2500, 0.008, 10000.0000, 5000000.0000, NULL, true),
('KHR', 'CASH_PICKUP', 'MONEY_GRAM', '머니그램 캐쉬픽업', 'EXPRESS', 5000, 0.020, 10000.0000, 1500000.0000, NULL, true),
('KHR', 'CASH_PICKUP', 'WING', 'Wing캐쉬픽업', 'EXPRESS', 5000, 0.019, 10000.0000, 1500000.0000, NULL, true),
('KHR', 'CARD', 'VISA', 'Visa카드', 'EXPRESS', 5000, 0.025, 10000.0000, 2000000.0000, '지원하지 않습니다.', true),
('KHR', 'WING', 'NONE', 'Wing계정', 'EXPRESS', 2500, 0.011, 10000.0000, 1000000.0000, NULL, true);
