USE bus_ticket;

-- =========================================================
-- 1) USERS
-- =========================================================
INSERT INTO users (name, email, password, phone, role, status)
VALUES
    ('Aung Myint Oo', 'aungmyintoo@busticket.com', SHA2('pass123', 256), '0922000001', 'PASSENGER', 'ACTIVE'),
    ('May Thazin', 'maythazin@busticket.com', SHA2('pass123', 256), '0922000002', 'PASSENGER', 'ACTIVE'),
    ('Ko Ko Aye', 'kokoaye@busticket.com', SHA2('pass123', 256), '0922000003', 'PASSENGER', 'ACTIVE'),
    ('Ei Mon', 'eimon@busticket.com', SHA2('pass123', 256), '0922000004', 'PASSENGER', 'ACTIVE'),
    ('Nyein Chan', 'nyeinchan@busticket.com', SHA2('pass123', 256), '0922000005', 'PASSENGER', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    phone = VALUES(phone),
    role = VALUES(role),
    status = VALUES(status);

-- =========================================================
-- 2) BUSES (exactly 10)
-- =========================================================
INSERT INTO buses (bus_number, bus_name, type, total_seats, is_active)
VALUES
    ('MMT-001', 'JJ Express', 'VIP', 28, 1),
    ('MMT-002', 'Elite Express', 'VIP', 28, 1),
    ('MMT-003', 'Mandalar Min', 'VIP', 28, 1),
    ('MMT-004', 'Shwe Mandalar', 'NORMAL', 40, 1),
    ('MMT-005', 'OK Express', 'NORMAL', 40, 1),
    ('MMT-006', 'Famous Traveller', 'NORMAL', 40, 1),
    ('MMT-007', 'Bagan Min Thar', 'NORMAL', 40, 1),
    ('MMT-008', 'Taung Paw Thar', 'NORMAL', 40, 1),
    ('MMT-009', 'Naypyitaw Express', 'VIP', 28, 1),
    ('MMT-010', 'Shan Yoma Line', 'VIP', 28, 1)
ON DUPLICATE KEY UPDATE
    bus_name = VALUES(bus_name),
    type = VALUES(type),
    total_seats = VALUES(total_seats),
    is_active = VALUES(is_active);

-- =========================================================
-- 3) ROUTES (exactly 30, Myanmar actual intercity/tourism routes)
-- =========================================================
CREATE TEMPORARY TABLE IF NOT EXISTS tmp_seed_routes (
    origin_city VARCHAR(120) NOT NULL,
    destination_city VARCHAR(120) NOT NULL,
    distance_km DECIMAL(10,2) NOT NULL,
    estimated_duration VARCHAR(40) NOT NULL,
    is_active TINYINT(1) NOT NULL,
    PRIMARY KEY (origin_city, destination_city)
);

TRUNCATE TABLE tmp_seed_routes;

INSERT INTO tmp_seed_routes (origin_city, destination_city, distance_km, estimated_duration, is_active)
VALUES
    ('Yangon (ရန်ကုန်)', 'Mandalay (မန္တလေး)', 620.00, '8h 30m', 1),
    ('Mandalay (မန္တလေး)', 'Yangon (ရန်ကုန်)', 620.00, '9h 00m', 1),
    ('Yangon (ရန်ကုန်)', 'Naypyidaw (နေပြည်တော်)', 370.00, '5h 30m', 1),
    ('Naypyidaw (နေပြည်တော်)', 'Yangon (ရန်ကုန်)', 370.00, '5h 30m', 1),
    ('Yangon (ရန်ကုန်)', 'Bagan (ပုဂံ)', 630.00, '9h 30m', 1),
    ('Bagan (ပုဂံ)', 'Yangon (ရန်ကုန်)', 630.00, '10h 00m', 1),
    ('Mandalay (မန္တလေး)', 'Bagan (ပုဂံ)', 180.00, '3h 00m', 1),
    ('Bagan (ပုဂံ)', 'Mandalay (မန္တလေး)', 180.00, '3h 00m', 1),
    ('Yangon (ရန်ကုန်)', 'Taunggyi (တောင်ကြီး)', 635.00, '10h 30m', 1),
    ('Taunggyi (တောင်ကြီး)', 'Yangon (ရန်ကုန်)', 635.00, '10h 30m', 1),
    ('Yangon (ရန်ကုန်)', 'Nyaung Shwe (Inle Lake) (ညောင်ရွှေ-အင်းလေး)', 660.00, '11h 00m', 1),
    ('Nyaung Shwe (Inle Lake) (ညောင်ရွှေ-အင်းလေး)', 'Yangon (ရန်ကုန်)', 660.00, '11h 00m', 1),
    ('Mandalay (မန္တလေး)', 'Pyin Oo Lwin (ပြင်ဦးလွင်)', 67.00, '1h 45m', 1),
    ('Pyin Oo Lwin (ပြင်ဦးလွင်)', 'Mandalay (မန္တလေး)', 67.00, '1h 45m', 1),
    ('Yangon (ရန်ကုန်)', 'Mawlamyine (မော်လမြိုင်)', 300.00, '6h 00m', 1),
    ('Mawlamyine (မော်လမြိုင်)', 'Yangon (ရန်ကုန်)', 300.00, '6h 00m', 1),
    ('Yangon (ရန်ကုန်)', 'Hpa-An (ဘားအံ)', 270.00, '5h 30m', 1),
    ('Hpa-An (ဘားအံ)', 'Yangon (ရန်ကုန်)', 270.00, '5h 30m', 1),
    ('Yangon (ရန်ကုန်)', 'Pathein (ပုသိမ်)', 190.00, '4h 00m', 1),
    ('Pathein (ပုသိမ်)', 'Yangon (ရန်ကုန်)', 190.00, '4h 00m', 1),
    ('Pathein (ပုသိမ်)', 'Ngwe Saung Beach (ငွေဆောင်ကမ်းခြေ)', 48.00, '1h 30m', 1),
    ('Ngwe Saung Beach (ငွေဆောင်ကမ်းခြေ)', 'Pathein (ပုသိမ်)', 48.00, '1h 30m', 1),
    ('Pathein (ပုသိမ်)', 'Chaungtha Beach (ချောင်းသာကမ်းခြေ)', 40.00, '1h 15m', 1),
    ('Chaungtha Beach (ချောင်းသာကမ်းခြေ)', 'Pathein (ပုသိမ်)', 40.00, '1h 15m', 1),
    ('Yangon (ရန်ကုန်)', 'Thandwe (Ngapali) (သံတွဲ-ငပလီ)', 450.00, '10h 30m', 1),
    ('Thandwe (Ngapali) (သံတွဲ-ငပလီ)', 'Yangon (ရန်ကုန်)', 450.00, '10h 30m', 1),
    ('Mandalay (မန္တလေး)', 'Lashio (လားရှိုး)', 260.00, '6h 00m', 1),
    ('Lashio (လားရှိုး)', 'Mandalay (မန္တလေး)', 260.00, '6h 00m', 1),
    ('Mandalay (မန္တလေး)', 'Myitkyina (မြစ်ကြီးနား)', 520.00, '12h 00m', 1),
    ('Myitkyina (မြစ်ကြီးနား)', 'Mandalay (မန္တလေး)', 520.00, '12h 00m', 1);

UPDATE routes r
JOIN tmp_seed_routes s
  ON r.origin_city = s.origin_city
 AND r.destination_city = s.destination_city
SET r.distance_km = s.distance_km,
    r.estimated_duration = s.estimated_duration,
    r.is_active = s.is_active
WHERE r.route_id > 0;

INSERT INTO routes (origin_city, destination_city, distance_km, estimated_duration, is_active)
SELECT s.origin_city, s.destination_city, s.distance_km, s.estimated_duration, s.is_active
FROM tmp_seed_routes s
LEFT JOIN routes r
  ON r.origin_city = s.origin_city
 AND r.destination_city = s.destination_city
WHERE r.route_id IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_routes;

-- =========================================================
-- 4) SEATS for seeded buses
-- =========================================================
INSERT IGNORE INTO seats (bus_id, seat_number)
SELECT
    b.bus_id,
    CONCAT(CHAR(65 + FLOOR((n.seq - 1) / 4)), ((n.seq - 1) % 4) + 1) AS seat_number
FROM buses b
JOIN (
    SELECT (ones.n + tens.n * 10 + 1) AS seq
    FROM
        (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
         UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
    CROSS JOIN
        (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5) tens
) n
  ON n.seq <= b.total_seats
WHERE b.bus_number LIKE 'MMT-%';

-- =========================================================
-- 5) TRIPS for next two weeks (14 days, daily full schedule for all 10 buses)
-- =========================================================
CREATE TEMPORARY TABLE IF NOT EXISTS tmp_trip_plan (
    bus_number VARCHAR(80) PRIMARY KEY,
    odd_origin VARCHAR(120) NOT NULL,
    odd_destination VARCHAR(120) NOT NULL,
    odd_departure TIME NOT NULL,
    odd_arrival TIME NOT NULL,
    odd_price DECIMAL(12,2) NOT NULL,
    even_origin VARCHAR(120) NOT NULL,
    even_destination VARCHAR(120) NOT NULL,
    even_departure TIME NOT NULL,
    even_arrival TIME NOT NULL,
    even_price DECIMAL(12,2) NOT NULL
);

TRUNCATE TABLE tmp_trip_plan;

INSERT INTO tmp_trip_plan (
    bus_number,
    odd_origin, odd_destination, odd_departure, odd_arrival, odd_price,
    even_origin, even_destination, even_departure, even_arrival, even_price
)
VALUES
    ('MMT-001', 'Yangon (ရန်ကုန်)', 'Mandalay (မန္တလေး)', '07:00:00', '15:30:00', 48000.00, 'Mandalay (မန္တလေး)', 'Yangon (ရန်ကုန်)', '07:00:00', '16:00:00', 48000.00),
    ('MMT-002', 'Yangon (ရန်ကုန်)', 'Bagan (ပုဂံ)', '07:30:00', '17:00:00', 46000.00, 'Bagan (ပုဂံ)', 'Yangon (ရန်ကုန်)', '07:30:00', '17:30:00', 46000.00),
    ('MMT-003', 'Yangon (ရန်ကုန်)', 'Nyaung Shwe (Inle Lake) (ညောင်ရွှေ-အင်းလေး)', '06:00:00', '17:00:00', 55000.00, 'Nyaung Shwe (Inle Lake) (ညောင်ရွှေ-အင်းလေး)', 'Yangon (ရန်ကုန်)', '06:00:00', '17:00:00', 55000.00),
    ('MMT-004', 'Yangon (ရန်ကုန်)', 'Taunggyi (တောင်ကြီး)', '06:30:00', '17:00:00', 52000.00, 'Taunggyi (တောင်ကြီး)', 'Yangon (ရန်ကုန်)', '06:30:00', '17:00:00', 52000.00),
    ('MMT-005', 'Yangon (ရန်ကုန်)', 'Mawlamyine (မော်လမြိုင်)', '08:00:00', '14:00:00', 25000.00, 'Mawlamyine (မော်လမြိုင်)', 'Yangon (ရန်ကုန်)', '08:00:00', '14:00:00', 25000.00),
    ('MMT-006', 'Yangon (ရန်ကုန်)', 'Hpa-An (ဘားအံ)', '08:30:00', '14:00:00', 24000.00, 'Hpa-An (ဘားအံ)', 'Yangon (ရန်ကုန်)', '08:30:00', '14:00:00', 24000.00),
    ('MMT-007', 'Yangon (ရန်ကုန်)', 'Pathein (ပုသိမ်)', '09:00:00', '13:00:00', 18000.00, 'Pathein (ပုသိမ်)', 'Yangon (ရန်ကုန်)', '09:00:00', '13:00:00', 18000.00),
    ('MMT-008', 'Pathein (ပုသိမ်)', 'Ngwe Saung Beach (ငွေဆောင်ကမ်းခြေ)', '10:00:00', '11:30:00', 8000.00, 'Ngwe Saung Beach (ငွေဆောင်ကမ်းခြေ)', 'Pathein (ပုသိမ်)', '10:00:00', '11:30:00', 8000.00),
    ('MMT-009', 'Yangon (ရန်ကုန်)', 'Thandwe (Ngapali) (သံတွဲ-ငပလီ)', '05:30:00', '16:00:00', 62000.00, 'Thandwe (Ngapali) (သံတွဲ-ငပလီ)', 'Yangon (ရန်ကုန်)', '05:30:00', '16:00:00', 62000.00),
    ('MMT-010', 'Mandalay (မန္တလေး)', 'Lashio (လားရှိုး)', '07:00:00', '13:00:00', 30000.00, 'Lashio (လားရှိုး)', 'Mandalay (မန္တလေး)', '07:00:00', '13:00:00', 30000.00);

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_next_two_weeks (
    travel_date DATE PRIMARY KEY
);

TRUNCATE TABLE tmp_next_two_weeks;

INSERT INTO tmp_next_two_weeks (travel_date)
SELECT DATE_ADD(CURDATE(), INTERVAL d.n DAY)
FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL
    SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL
    SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL
    SELECT 13 UNION ALL SELECT 14
) d;

INSERT INTO trips (bus_id, route_id, travel_date, departure_time, arrival_time, price, status)
SELECT
    b.bus_id,
    rm.route_id,
    d.travel_date,
    CASE
        WHEN MOD(DAYOFYEAR(d.travel_date), 2) = 1 THEN p.odd_departure
        ELSE p.even_departure
    END AS departure_time,
    CASE
        WHEN MOD(DAYOFYEAR(d.travel_date), 2) = 1 THEN p.odd_arrival
        ELSE p.even_arrival
    END AS arrival_time,
    CASE
        WHEN MOD(DAYOFYEAR(d.travel_date), 2) = 1 THEN p.odd_price
        ELSE p.even_price
    END AS price,
    'OPEN' AS status
FROM tmp_trip_plan p
JOIN buses b
  ON b.bus_number = p.bus_number
JOIN tmp_next_two_weeks d
JOIN (
    SELECT origin_city, destination_city, MIN(route_id) AS route_id
    FROM routes
    GROUP BY origin_city, destination_city
) rm
  ON rm.origin_city = CASE WHEN MOD(DAYOFYEAR(d.travel_date), 2) = 1 THEN p.odd_origin ELSE p.even_origin END
 AND rm.destination_city = CASE WHEN MOD(DAYOFYEAR(d.travel_date), 2) = 1 THEN p.odd_destination ELSE p.even_destination END
LEFT JOIN trips t
  ON t.bus_id = b.bus_id
 AND t.travel_date = d.travel_date
 AND t.departure_time = CASE
        WHEN MOD(DAYOFYEAR(d.travel_date), 2) = 1 THEN p.odd_departure
        ELSE p.even_departure
    END
WHERE t.trip_id IS NULL;

-- Extra daily trips: same route, different bus, different time (another 5 + 5 per day)
CREATE TEMPORARY TABLE IF NOT EXISTS tmp_trip_plan_extra (
    bus_number VARCHAR(80) PRIMARY KEY,
    odd_origin VARCHAR(120) NOT NULL,
    odd_destination VARCHAR(120) NOT NULL,
    odd_departure TIME NOT NULL,
    odd_arrival TIME NOT NULL,
    odd_price DECIMAL(12,2) NOT NULL,
    even_origin VARCHAR(120) NOT NULL,
    even_destination VARCHAR(120) NOT NULL,
    even_departure TIME NOT NULL,
    even_arrival TIME NOT NULL,
    even_price DECIMAL(12,2) NOT NULL
);

TRUNCATE TABLE tmp_trip_plan_extra;

INSERT INTO tmp_trip_plan_extra (
    bus_number,
    odd_origin, odd_destination, odd_departure, odd_arrival, odd_price,
    even_origin, even_destination, even_departure, even_arrival, even_price
)
VALUES
    -- Yangon <-> Mandalay (extra 5 trips)
    ('MMT-001', 'Yangon (ရန်ကုန်)', 'Mandalay (မန္တလေး)', '15:00:00', '23:30:00', 47000.00, 'Mandalay (မန္တလေး)', 'Yangon (ရန်ကုန်)', '15:00:00', '23:59:00', 47000.00),
    ('MMT-002', 'Yangon (ရန်ကုန်)', 'Mandalay (မန္တလေး)', '15:30:00', '23:59:00', 48000.00, 'Mandalay (မန္တလေး)', 'Yangon (ရန်ကုန်)', '15:30:00', '23:59:00', 48000.00),
    ('MMT-003', 'Yangon (ရန်ကုန်)', 'Mandalay (မန္တလေး)', '16:00:00', '23:59:00', 49000.00, 'Mandalay (မန္တလေး)', 'Yangon (ရန်ကုန်)', '16:00:00', '23:59:00', 49000.00),
    ('MMT-004', 'Yangon (ရန်ကုန်)', 'Mandalay (မန္တလေး)', '16:30:00', '23:59:00', 45000.00, 'Mandalay (မန္တလေး)', 'Yangon (ရန်ကုန်)', '16:30:00', '23:59:00', 45000.00),
    ('MMT-005', 'Yangon (ရန်ကုန်)', 'Mandalay (မန္တလေး)', '17:00:00', '23:59:00', 44000.00, 'Mandalay (မန္တလေး)', 'Yangon (ရန်ကုန်)', '17:00:00', '23:59:00', 44000.00),

    -- Yangon <-> Bagan (extra 5 trips)
    ('MMT-006', 'Yangon (ရန်ကုန်)', 'Bagan (ပုဂံ)', '15:00:00', '23:59:00', 45000.00, 'Bagan (ပုဂံ)', 'Yangon (ရန်ကုန်)', '15:00:00', '23:59:00', 45000.00),
    ('MMT-007', 'Yangon (ရန်ကုန်)', 'Bagan (ပုဂံ)', '15:30:00', '23:59:00', 46000.00, 'Bagan (ပုဂံ)', 'Yangon (ရန်ကုန်)', '15:30:00', '23:59:00', 46000.00),
    ('MMT-008', 'Yangon (ရန်ကုန်)', 'Bagan (ပုဂံ)', '16:00:00', '23:59:00', 43000.00, 'Bagan (ပုဂံ)', 'Yangon (ရန်ကုန်)', '16:00:00', '23:59:00', 43000.00),
    ('MMT-009', 'Yangon (ရန်ကုန်)', 'Bagan (ပုဂံ)', '16:30:00', '23:59:00', 47000.00, 'Bagan (ပုဂံ)', 'Yangon (ရန်ကုန်)', '16:30:00', '23:59:00', 47000.00),
    ('MMT-010', 'Yangon (ရန်ကုန်)', 'Bagan (ပုဂံ)', '17:00:00', '23:59:00', 48000.00, 'Bagan (ပုဂံ)', 'Yangon (ရန်ကုန်)', '17:00:00', '23:59:00', 48000.00);

INSERT INTO trips (bus_id, route_id, travel_date, departure_time, arrival_time, price, status)
SELECT
    b.bus_id,
    rm.route_id,
    d.travel_date,
    CASE
        WHEN MOD(DAYOFYEAR(d.travel_date), 2) = 1 THEN p.odd_departure
        ELSE p.even_departure
    END AS departure_time,
    CASE
        WHEN MOD(DAYOFYEAR(d.travel_date), 2) = 1 THEN p.odd_arrival
        ELSE p.even_arrival
    END AS arrival_time,
    CASE
        WHEN MOD(DAYOFYEAR(d.travel_date), 2) = 1 THEN p.odd_price
        ELSE p.even_price
    END AS price,
    'OPEN' AS status
FROM tmp_trip_plan_extra p
JOIN buses b
  ON b.bus_number = p.bus_number
JOIN tmp_next_two_weeks d
JOIN (
    SELECT origin_city, destination_city, MIN(route_id) AS route_id
    FROM routes
    GROUP BY origin_city, destination_city
) rm
  ON rm.origin_city = CASE WHEN MOD(DAYOFYEAR(d.travel_date), 2) = 1 THEN p.odd_origin ELSE p.even_origin END
 AND rm.destination_city = CASE WHEN MOD(DAYOFYEAR(d.travel_date), 2) = 1 THEN p.odd_destination ELSE p.even_destination END
LEFT JOIN trips t
  ON t.bus_id = b.bus_id
 AND t.travel_date = d.travel_date
 AND t.departure_time = CASE
        WHEN MOD(DAYOFYEAR(d.travel_date), 2) = 1 THEN p.odd_departure
        ELSE p.even_departure
    END
WHERE t.trip_id IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_trip_plan_extra;
DROP TEMPORARY TABLE IF EXISTS tmp_next_two_weeks;
DROP TEMPORARY TABLE IF EXISTS tmp_trip_plan;

