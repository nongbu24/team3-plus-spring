-- 프론트 테스트용 회원: user@example.com / password1234!
INSERT INTO users (id, email, password, name, phone, role, created_at, updated_at, deleted_at)
VALUES
    (100, 'user@example.com', '$2a$10$Q/KEclvina.r4fA46zrHSegLUGewXAU8OwXRVmyNXyWc8t0xE6fOS', '테스트회원', '010-1234-5678', 'USER', NOW(), NULL, NULL);

INSERT INTO carts (id, user_id, created_at, updated_at)
VALUES
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
