INSERT INTO categories (id, name, is_active, created_at, updated_at)
VALUES
    (1, '스마트폰', true, NOW(), NULL),
    (2, '태블릿', true, NOW(), NULL),
    (3, '노트북', true, NOW(), NULL),
    (4, '데스크탑', true, NOW(), NULL),
    (5, '모니터', true, NOW(), NULL),
    (6, '키보드/마우스', true, NOW(), NULL),
    (7, '이어폰/헤드폰', true, NOW(), NULL),
    (8, '스피커', true, NOW(), NULL),
    (9, '스마트워치', true, NOW(), NULL),
    (10, '카메라', true, NOW(), NULL),
    (11, '게임기/콘솔', true, NOW(), NULL),
    (12, '저장장치', true, NOW(), NULL),
    (13, '네트워크 장비', true, NOW(), NULL),
    (14, '충전기/케이블', true, NOW(), NULL),
    (15, 'PC 부품', true, NOW(), NULL),
    (16, '생활가전', true, NOW(), NULL),
    (17, '주방가전', true, NOW(), NULL),
    (18, '계절가전', true, NOW(), NULL),
    (19, '액세서리', true, NOW(), NULL),
    (20, '기타 전자제품', true, NOW(), NULL);

INSERT INTO products (id, category_id, name, description, price, stock, status, created_at, updated_at)
VALUES
    (1, 1, 'Galaxy S25 256GB', '6.7인치 AMOLED 디스플레이와 고성능 카메라를 갖춘 스마트폰입니다.', 1290000, 35, 'ON_SALE', NOW(), NULL),
    (2, 1, 'iPhone 16 128GB', '일상 촬영과 빠른 앱 실행에 적합한 프리미엄 스마트폰입니다.', 1250000, 28, 'ON_SALE', NOW(), NULL),
    (3, 1, 'Pixel 9 Pro 256GB', '깔끔한 안드로이드 경험과 사진 보정 기능이 강점인 스마트폰입니다.', 1190000, 14, 'ON_SALE', NOW(), NULL),
    (4, 2, 'Galaxy Tab S10 11형', '영상 시청, 필기, 가벼운 업무에 적합한 태블릿입니다.', 890000, 22, 'ON_SALE', NOW(), NULL),
    (5, 2, 'iPad Air 11형 128GB', '얇고 가벼운 디자인의 다목적 태블릿입니다.', 929000, 18, 'ON_SALE', NOW(), NULL),
    (6, 2, 'Lenovo Tab Plus 128GB', '가성비 좋은 엔터테인먼트용 안드로이드 태블릿입니다.', 329000, 40, 'ON_SALE', NOW(), NULL),
    (7, 3, 'LG Gram 16 2026', '가벼운 무게와 긴 배터리 시간을 제공하는 16형 노트북입니다.', 1790000, 12, 'ON_SALE', NOW(), NULL),
    (8, 3, 'MacBook Air 13 M4', '휴대성과 성능을 모두 갖춘 13형 노트북입니다.', 1590000, 16, 'ON_SALE', NOW(), NULL),
    (9, 3, 'ThinkPad X1 Carbon', '업무용으로 안정적인 성능과 내구성을 갖춘 노트북입니다.', 2190000, 7, 'ON_SALE', NOW(), NULL),
    (10, 3, 'ASUS VivoBook 15', '학생과 사무용 사용자에게 적합한 실속형 노트북입니다.', 790000, 25, 'ON_SALE', NOW(), NULL),
    (11, 4, 'HP Pavilion Desktop', '가정과 사무실에서 사용하기 좋은 데스크탑 PC입니다.', 890000, 10, 'ON_SALE', NOW(), NULL),
    (12, 4, 'Gaming Desktop RTX 4060', 'FHD 게이밍과 영상 편집에 적합한 게이밍 데스크탑입니다.', 1490000, 6, 'ON_SALE', NOW(), NULL),
    (13, 5, 'Samsung Smart Monitor M7 32형', '업무와 콘텐츠 감상을 함께 할 수 있는 스마트 모니터입니다.', 459000, 20, 'ON_SALE', NOW(), NULL),
    (14, 5, 'LG UltraGear 27형 144Hz', '부드러운 화면 전환을 지원하는 게이밍 모니터입니다.', 399000, 17, 'ON_SALE', NOW(), NULL),
    (15, 5, 'Dell UltraSharp 27형 QHD', '색 정확도가 뛰어난 업무용 QHD 모니터입니다.', 529000, 9, 'ON_SALE', NOW(), NULL),
    (16, 6, 'Logitech MX Keys S', '조용한 타건감과 멀티 디바이스 연결을 지원하는 키보드입니다.', 159000, 30, 'ON_SALE', NOW(), NULL),
    (17, 6, 'Logitech MX Master 3S', '정밀한 스크롤과 편안한 그립감을 제공하는 무선 마우스입니다.', 139000, 32, 'ON_SALE', NOW(), NULL),
    (18, 6, 'Keychron K8 Pro', '핫스왑을 지원하는 텐키리스 기계식 키보드입니다.', 149000, 15, 'ON_SALE', NOW(), NULL),
    (19, 7, 'AirPods Pro 3', '노이즈 캔슬링과 공간 음향을 지원하는 무선 이어폰입니다.', 359000, 24, 'ON_SALE', NOW(), NULL),
    (20, 7, 'Sony WH-1000XM6', '강력한 노이즈 캔슬링을 제공하는 무선 헤드폰입니다.', 499000, 13, 'ON_SALE', NOW(), NULL),
    (21, 7, 'Galaxy Buds 4 Pro', '안정적인 착용감과 선명한 통화 품질을 제공하는 이어폰입니다.', 269000, 26, 'ON_SALE', NOW(), NULL),
    (22, 8, 'JBL Flip 7', '야외에서 사용하기 좋은 휴대용 블루투스 스피커입니다.', 169000, 21, 'ON_SALE', NOW(), NULL),
    (23, 8, 'Bose SoundLink Max', '풍부한 저음과 선명한 사운드를 제공하는 블루투스 스피커입니다.', 459000, 8, 'ON_SALE', NOW(), NULL),
    (24, 9, 'Apple Watch Series 11', '운동 기록과 건강 관리를 지원하는 스마트워치입니다.', 599000, 19, 'ON_SALE', NOW(), NULL),
    (25, 9, 'Galaxy Watch 8', '수면, 운동, 심박 측정을 지원하는 안드로이드 스마트워치입니다.', 399000, 23, 'ON_SALE', NOW(), NULL),
    (26, 10, 'Canon EOS R50 Kit', '입문자에게 적합한 미러리스 카메라 렌즈 키트입니다.', 899000, 5, 'ON_SALE', NOW(), NULL),
    (27, 10, 'Sony ZV-E10 II', '브이로그와 영상 촬영에 적합한 미러리스 카메라입니다.', 1090000, 4, 'ON_SALE', NOW(), NULL),
    (28, 11, 'PlayStation 5 Slim', '고성능 콘솔 게임을 즐길 수 있는 플레이스테이션 본체입니다.', 688000, 11, 'ON_SALE', NOW(), NULL),
    (29, 11, 'Nintendo Switch OLED', '휴대와 거치 플레이를 모두 지원하는 콘솔 게임기입니다.', 415000, 18, 'ON_SALE', NOW(), NULL),
    (30, 12, 'Samsung 990 PRO SSD 1TB', '빠른 읽기/쓰기 속도를 제공하는 NVMe SSD입니다.', 159000, 34, 'ON_SALE', NOW(), NULL),
    (31, 12, 'WD My Passport 2TB', '사진과 문서를 보관하기 좋은 휴대용 외장하드입니다.', 119000, 27, 'ON_SALE', NOW(), NULL),
    (32, 12, 'SanDisk Extreme Portable SSD 1TB', '휴대성과 내구성을 갖춘 외장 SSD입니다.', 179000, 16, 'ON_SALE', NOW(), NULL),
    (33, 13, 'ipTIME AX3000 공유기', 'Wi-Fi 6를 지원하는 가정용 무선 공유기입니다.', 89000, 38, 'ON_SALE', NOW(), NULL),
    (34, 13, 'TP-Link Deco Mesh 2팩', '넓은 공간에서 안정적인 와이파이를 제공하는 메시 공유기입니다.', 229000, 12, 'ON_SALE', NOW(), NULL),
    (35, 14, '65W GaN 고속 충전기', '노트북과 스마트폰을 함께 충전할 수 있는 고속 충전기입니다.', 49000, 55, 'ON_SALE', NOW(), NULL),
    (36, 14, 'USB-C to C 케이블 2m', '고속 충전과 데이터 전송을 지원하는 USB-C 케이블입니다.', 15000, 100, 'ON_SALE', NOW(), NULL),
    (37, 14, 'MagSafe 무선 충전 패드', '자석 정렬을 지원하는 무선 충전 패드입니다.', 59000, 42, 'ON_SALE', NOW(), NULL),
    (38, 15, 'Intel Core i7 프로세서', '게이밍과 작업용 PC에 적합한 고성능 CPU입니다.', 459000, 9, 'ON_SALE', NOW(), NULL),
    (39, 15, 'NVIDIA RTX 4070 그래픽카드', 'QHD 게이밍과 그래픽 작업에 적합한 그래픽카드입니다.', 899000, 3, 'ON_SALE', NOW(), NULL),
    (40, 15, 'DDR5 32GB 메모리', '고성능 데스크탑 구성을 위한 DDR5 메모리입니다.', 149000, 20, 'ON_SALE', NOW(), NULL),
    (41, 16, 'Dyson V12 무선 청소기', '강력한 흡입력을 제공하는 무선 청소기입니다.', 749000, 8, 'ON_SALE', NOW(), NULL),
    (42, 16, 'LG 코드제로 로봇청소기', '자동 청소와 앱 제어를 지원하는 로봇청소기입니다.', 699000, 6, 'ON_SALE', NOW(), NULL),
    (43, 17, '쿠쿠 전기압력밥솥 6인용', '가정용으로 적합한 6인용 전기압력밥솥입니다.', 249000, 13, 'ON_SALE', NOW(), NULL),
    (44, 17, '필립스 에어프라이어 5L', '기름을 줄여 간편하게 조리할 수 있는 에어프라이어입니다.', 179000, 15, 'ON_SALE', NOW(), NULL),

    -- 품절 상품 5개
    (45, 18, 'LG 휘센 제습기 20L', '습한 계절에 실내 습도를 관리하기 좋은 제습기입니다.', 429000, 0, 'SOLD_OUT', NOW(), NULL),
    (46, 18, '신일 서큘레이터', '공기 순환에 적합한 저소음 서큘레이터입니다.', 99000, 0, 'SOLD_OUT', NOW(), NULL),
    (47, 19, '노트북 파우치 15형', '노트북을 안전하게 보관할 수 있는 기본형 파우치입니다.', 29000, 0, 'SOLD_OUT', NOW(), NULL),
    (48, 19, '스마트폰 투명 케이스', '깔끔한 디자인의 충격 보호 투명 케이스입니다.', 12000, 0, 'SOLD_OUT', NOW(), NULL),
    (49, 20, '전자 메모패드 12형', '간단한 메모와 그림을 남길 수 있는 전자 메모패드입니다.', 39000, 0, 'SOLD_OUT', NOW(), NULL),

    -- 단종 상품 3개
    (50, 20, '휴대용 미니 빔프로젝터 구형', '판매가 종료된 구형 미니 빔프로젝터입니다.', 299000, 0, 'DISCONTINUED', NOW(), NULL),
    (51, 1, 'Galaxy S20 리퍼 상품', '판매가 종료된 구형 스마트폰 리퍼 상품입니다.', 399000, 0, 'DISCONTINUED', NOW(), NULL),
    (52, 3, '구형 사무용 노트북 14형', '더 이상 판매하지 않는 구형 사무용 노트북입니다.', 499000, 0, 'DISCONTINUED', NOW(), NULL),

    -- 결제 테스트용 상품
    (53, 20, '결제 테스트 상품 1000원', 'PortOne 결제 검증 테스트를 위한 1,000원 상품입니다.', 1000, 999, 'ON_SALE', NOW(), NULL);

INSERT INTO users (id, email, password, name, phone, role, created_at, updated_at, deleted_at)
VALUES
    (1, 'admin@example.com', '$2a$10$Q/KEclvina.r4fA46zrHSegLUGewXAU8OwXRVmyNXyWc8t0xE6fOS', '관리자', '010-0000-0001', 'ADMIN', NOW(), NULL, NULL),
    (2, 'user01@example.com', '$2a$10$Q/KEclvina.r4fA46zrHSegLUGewXAU8OwXRVmyNXyWc8t0xE6fOS', '김민준', '010-1000-0001', 'USER', NOW(), NULL, NULL),
    (3, 'user02@example.com', '$2a$10$Q/KEclvina.r4fA46zrHSegLUGewXAU8OwXRVmyNXyWc8t0xE6fOS', '이서연', '010-1000-0002', 'USER', NOW(), NULL, NULL),
    (4, 'user03@example.com', '$2a$10$Q/KEclvina.r4fA46zrHSegLUGewXAU8OwXRVmyNXyWc8t0xE6fOS', '박지훈', '010-1000-0003', 'USER', NOW(), NULL, NULL),
    (5, 'user04@example.com', '$2a$10$Q/KEclvina.r4fA46zrHSegLUGewXAU8OwXRVmyNXyWc8t0xE6fOS', '최하은', '010-1000-0004', 'USER', NOW(), NULL, NULL),
    (6, 'user05@example.com', '$2a$10$Q/KEclvina.r4fA46zrHSegLUGewXAU8OwXRVmyNXyWc8t0xE6fOS', '정도윤', '010-1000-0005', 'USER', NOW(), NULL, NULL),
    (7, 'user06@example.com', '$2a$10$Q/KEclvina.r4fA46zrHSegLUGewXAU8OwXRVmyNXyWc8t0xE6fOS', '강수아', '010-1000-0006', 'USER', NOW(), NULL, NULL),
    (8, 'user07@example.com', '$2a$10$Q/KEclvina.r4fA46zrHSegLUGewXAU8OwXRVmyNXyWc8t0xE6fOS', '조현우', '010-1000-0007', 'USER', NOW(), NULL, NULL),
    (9, 'user08@example.com', '$2a$10$Q/KEclvina.r4fA46zrHSegLUGewXAU8OwXRVmyNXyWc8t0xE6fOS', '윤지아', '010-1000-0008', 'USER', NOW(), NULL, NULL),
    (10, 'user09@example.com', '$2a$10$Q/KEclvina.r4fA46zrHSegLUGewXAU8OwXRVmyNXyWc8t0xE6fOS', '장서준', '010-1000-0009', 'USER', NOW(), NULL, NULL),
    (11, 'user10@example.com', '$2a$10$Q/KEclvina.r4fA46zrHSegLUGewXAU8OwXRVmyNXyWc8t0xE6fOS', '임채원', '010-1000-0010', 'USER', NOW(), NULL, NULL);

-- 프론트 테스트용 회원: user@example.com / password1234!
INSERT INTO users (id, email, password, name, phone, role, created_at, updated_at, deleted_at)
VALUES
    (100, 'user@example.com', '$2a$10$Q/KEclvina.r4fA46zrHSegLUGewXAU8OwXRVmyNXyWc8t0xE6fOS', '테스트회원', '010-1234-5678', 'USER', NOW(), NULL, NULL);

INSERT INTO carts (id, user_id, created_at, updated_at)
VALUES
    (1, 1, NOW(), NULL),
    (2, 2, NOW(), NULL),
    (3, 3, NOW(), NULL),
    (4, 4, NOW(), NULL),
    (5, 5, NOW(), NULL),
    (6, 6, NOW(), NULL),
    (7, 7, NOW(), NULL),
    (8, 8, NOW(), NULL),
    (9, 9, NOW(), NULL),
    (10, 10, NOW(), NULL),
    (11, 11, NOW(), NULL),
    (100, 100, NOW(), NULL);

INSERT INTO coupon_events (id, name, discount_type, discount_amount, total_quantity, issued_quantity, status, starts_at, ends_at, valid_days, created_at, updated_at)
VALUES
    (1001, '테스트 1000원 쿠폰', 'FIXED', 1000, 100, 1, 'OPEN', NOW() - INTERVAL '1' DAY, NOW() + INTERVAL '30' DAY, 30, NOW(), NULL),
    (1002, '테스트 2000원 쿠폰', 'FIXED', 2000, 100, 1, 'OPEN', NOW() - INTERVAL '1' DAY, NOW() + INTERVAL '30' DAY, 30, NOW(), NULL);

INSERT INTO user_coupons (id, user_id, coupon_event_id, order_id, status, issued_at, used_at, expired_at)
VALUES
    (1001, 100, 1001, NULL, 'ISSUED', NOW(), NULL, NOW() + INTERVAL '30' DAY),
    (1002, 100, 1002, NULL, 'ISSUED', NOW(), NULL, NOW() + INTERVAL '30' DAY);

INSERT INTO orders (id, user_id, order_number, status, total_product_amount, used_coupon_amount, payment_amount, canceled_at, created_at, updated_at)
VALUES
    (1001, 100, 'order_seed_user_001', 'COMPLETED', 2190000, 0, 2190000, NULL, NOW() - INTERVAL '10' DAY, NULL),
    (1002, 100, 'order_seed_user_002', 'COMPLETED', 1790000, 0, 1790000, NULL, NOW() - INTERVAL '9' DAY, NULL),
    (1003, 100, 'order_seed_user_003', 'READY', 1590000, 0, 1590000, NULL, NOW() - INTERVAL '8' DAY, NULL),
    (1004, 100, 'order_seed_user_004', 'READY', 1490000, 0, 1490000, NULL, NOW() - INTERVAL '7' DAY, NULL),
    (1005, 100, 'order_seed_user_005', 'READY', 1290000, 0, 1290000, NULL, NOW() - INTERVAL '6' DAY, NULL),
    (1006, 100, 'order_seed_user_006', 'READY', 1250000, 0, 1250000, NULL, NOW() - INTERVAL '5' DAY, NULL),
    (1007, 100, 'order_seed_user_007', 'READY', 929000, 0, 929000, NULL, NOW() - INTERVAL '4' DAY, NULL),
    (1008, 100, 'order_seed_user_008', 'READY', 599000, 0, 599000, NULL, NOW() - INTERVAL '3' DAY, NULL),
    (1009, 100, 'order_seed_user_009', 'READY', 499000, 0, 499000, NULL, NOW() - INTERVAL '2' DAY, NULL),
    (1010, 100, 'order_seed_user_010', 'READY', 229000, 0, 229000, NULL, NOW() - INTERVAL '1' DAY, NULL);

INSERT INTO order_items (id, order_id, product_id, product_name, unit_price, quantity, line_amount, created_at, updated_at)
VALUES
    (1001, 1001, 9, 'ThinkPad X1 Carbon', 2190000, 1, 2190000, NOW() - INTERVAL '10' DAY, NULL),
    (1002, 1002, 7, 'LG Gram 16 2026', 1790000, 1, 1790000, NOW() - INTERVAL '9' DAY, NULL),
    (1003, 1003, 8, 'MacBook Air 13 M4', 1590000, 1, 1590000, NOW() - INTERVAL '8' DAY, NULL),
    (1004, 1004, 12, 'Gaming Desktop RTX 4060', 1490000, 1, 1490000, NOW() - INTERVAL '7' DAY, NULL),
    (1005, 1005, 1, 'Galaxy S25 256GB', 1290000, 1, 1290000, NOW() - INTERVAL '6' DAY, NULL),
    (1006, 1006, 2, 'iPhone 16 128GB', 1250000, 1, 1250000, NOW() - INTERVAL '5' DAY, NULL),
    (1007, 1007, 5, 'iPad Air 11형 128GB', 929000, 1, 929000, NOW() - INTERVAL '4' DAY, NULL),
    (1008, 1008, 24, 'Apple Watch Series 11', 599000, 1, 599000, NOW() - INTERVAL '3' DAY, NULL),
    (1009, 1009, 20, 'Sony WH-1000XM6', 499000, 1, 499000, NOW() - INTERVAL '2' DAY, NULL),
    (1010, 1010, 34, 'TP-Link Deco Mesh 2팩', 229000, 1, 229000, NOW() - INTERVAL '1' DAY, NULL);

INSERT INTO payments (id, order_id, portone_payment_id, status, total_product_amount, used_coupon_amount, payment_amount, approved_at, created_at, updated_at)
VALUES
    (1001, 1001, 'pay_seed_user_001', 'PAID', 2190000, 0, 2190000, NOW() - INTERVAL '10' DAY, NOW() - INTERVAL '10' DAY, NULL),
    (1002, 1002, 'pay_seed_user_002', 'PAID', 1790000, 0, 1790000, NOW() - INTERVAL '9' DAY, NOW() - INTERVAL '9' DAY, NULL),
    (1003, 1003, 'pay_seed_user_003', 'PENDING', 1590000, 0, 1590000, NULL, NOW() - INTERVAL '8' DAY, NULL),
    (1004, 1004, 'pay_seed_user_004', 'PENDING', 1490000, 0, 1490000, NULL, NOW() - INTERVAL '7' DAY, NULL),
    (1005, 1005, 'pay_seed_user_005', 'PENDING', 1290000, 0, 1290000, NULL, NOW() - INTERVAL '6' DAY, NULL),
    (1006, 1006, 'pay_seed_user_006', 'PENDING', 1250000, 0, 1250000, NULL, NOW() - INTERVAL '5' DAY, NULL),
    (1007, 1007, 'pay_seed_user_007', 'PENDING', 929000, 0, 929000, NULL, NOW() - INTERVAL '4' DAY, NULL),
    (1008, 1008, 'pay_seed_user_008', 'PENDING', 599000, 0, 599000, NULL, NOW() - INTERVAL '3' DAY, NULL),
    (1009, 1009, 'pay_seed_user_009', 'PENDING', 499000, 0, 499000, NULL, NOW() - INTERVAL '2' DAY, NULL),
    (1010, 1010, 'pay_seed_user_010', 'PENDING', 229000, 0, 229000, NULL, NOW() - INTERVAL '1' DAY, NULL);
