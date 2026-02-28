USE bus_ticket;

INSERT INTO users (name, email, password, phone, role, status)
VALUES
    ('Maya Thant', 'admin@busticket.com', SHA2('pass123', 256), '0911111111', 'ADMIN', 'ACTIVE'),
    ('Ko Min', 'komin@gmail.com', SHA2('pass123', 256), '0922222222', 'PASSENGER', 'ACTIVE'),
    ('Aye Aye', 'ayeaye@gmail.com', SHA2('pass123', 256), '0933333333', 'PASSENGER', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    phone = VALUES(phone),
    role = VALUES(role),
    status = VALUES(status);

INSERT INTO buses (bus_number, bus_name, type, total_seats, is_active)
VALUES
    ('VIP-1001', 'Golden Express', 'VIP', 28, 1),
    ('VIP-1002', 'Silver Line', 'VIP', 28, 1),
    ('NORM-2001', 'City Rider', 'NORMAL', 40, 1)
ON DUPLICATE KEY UPDATE
    bus_name = VALUES(bus_name),
    type = VALUES(type),
    total_seats = VALUES(total_seats),
    is_active = VALUES(is_active);

INSERT INTO routes (origin_city, destination_city, distance_km, estimated_duration, is_active)
SELECT 'Yangon', 'Mandalay', 620.00, '8h 30m', 1
WHERE NOT EXISTS (
    SELECT 1 FROM routes
    WHERE origin_city = 'Yangon'
      AND destination_city = 'Mandalay'
      AND distance_km = 620.00
      AND estimated_duration = '8h 30m'
);

INSERT INTO routes (origin_city, destination_city, distance_km, estimated_duration, is_active)
SELECT 'Yangon', 'Naypyidaw', 370.00, '5h 30m', 1
WHERE NOT EXISTS (
    SELECT 1 FROM routes
    WHERE origin_city = 'Yangon'
      AND destination_city = 'Naypyidaw'
      AND distance_km = 370.00
      AND estimated_duration = '5h 30m'
);

INSERT INTO routes (origin_city, destination_city, distance_km, estimated_duration, is_active)
SELECT 'Mandalay', 'Bagan', 180.00, '3h 00m', 1
WHERE NOT EXISTS (
    SELECT 1 FROM routes
    WHERE origin_city = 'Mandalay'
      AND destination_city = 'Bagan'
      AND distance_km = 180.00
      AND estimated_duration = '3h 00m'
);

INSERT INTO routes (origin_city, destination_city, distance_km, estimated_duration, is_active)
SELECT 'Naypyidaw', 'Mandalay', 260.00, '4h 10m', 1
WHERE NOT EXISTS (
    SELECT 1 FROM routes
    WHERE origin_city = 'Naypyidaw'
      AND destination_city = 'Mandalay'
      AND distance_km = 260.00
      AND estimated_duration = '4h 10m'
);

INSERT INTO trips (bus_id, route_id, travel_date, departure_time, arrival_time, price, status)
SELECT b.bus_id, r.route_id, '2026-03-15', '08:00:00', '16:30:00', 35000.00, 'OPEN'
FROM buses b
JOIN routes r ON r.origin_city = 'Yangon' AND r.destination_city = 'Mandalay'
WHERE b.bus_number = 'VIP-1001'
ON DUPLICATE KEY UPDATE
    arrival_time = VALUES(arrival_time),
    price = VALUES(price),
    status = VALUES(status);

INSERT INTO trips (bus_id, route_id, travel_date, departure_time, arrival_time, price, status)
SELECT b.bus_id, r.route_id, '2026-03-15', '09:30:00', '15:00:00', 22000.00, 'OPEN'
FROM buses b
JOIN routes r ON r.origin_city = 'Yangon' AND r.destination_city = 'Naypyidaw'
WHERE b.bus_number = 'NORM-2001'
ON DUPLICATE KEY UPDATE
    arrival_time = VALUES(arrival_time),
    price = VALUES(price),
    status = VALUES(status);

INSERT INTO trips (bus_id, route_id, travel_date, departure_time, arrival_time, price, status)
SELECT b.bus_id, r.route_id, '2026-03-16', '07:00:00', '10:00:00', 14000.00, 'OPEN'
FROM buses b
JOIN routes r ON r.origin_city = 'Mandalay' AND r.destination_city = 'Bagan'
WHERE b.bus_number = 'VIP-1002'
ON DUPLICATE KEY UPDATE
    arrival_time = VALUES(arrival_time),
    price = VALUES(price),
    status = VALUES(status);

INSERT INTO trips (bus_id, route_id, travel_date, departure_time, arrival_time, price, status)
SELECT b.bus_id, r.route_id, '2026-03-16', '13:00:00', '17:10:00', 20000.00, 'OPEN'
FROM buses b
JOIN routes r ON r.origin_city = 'Naypyidaw' AND r.destination_city = 'Mandalay'
WHERE b.bus_number = 'NORM-2001'
ON DUPLICATE KEY UPDATE
    arrival_time = VALUES(arrival_time),
    price = VALUES(price),
    status = VALUES(status);

INSERT IGNORE INTO seats (bus_id, seat_number)
SELECT b.bus_id, s.seat_number
FROM buses b
CROSS JOIN (
    SELECT 'A1' AS seat_number UNION ALL SELECT 'A2' UNION ALL SELECT 'A3' UNION ALL SELECT 'A4' UNION ALL
    SELECT 'B1' UNION ALL SELECT 'B2' UNION ALL SELECT 'B3' UNION ALL SELECT 'B4' UNION ALL
    SELECT 'C1' UNION ALL SELECT 'C2' UNION ALL SELECT 'C3' UNION ALL SELECT 'C4'
) s
WHERE b.bus_number IN ('VIP-1001', 'VIP-1002', 'NORM-2001');

INSERT INTO bookings (user_id, trip_id, total_price, ticket_code, status)
SELECT u.user_id, t.trip_id, t.price, 'TCK-10001', 'CONFIRMED'
FROM users u
JOIN trips t ON t.travel_date = '2026-03-15' AND t.departure_time = '08:00:00'
WHERE u.email = 'maya.passenger@busticket.com'
  AND NOT EXISTS (SELECT 1 FROM bookings WHERE ticket_code = 'TCK-10001');

INSERT INTO bookings (user_id, trip_id, total_price, ticket_code, status)
SELECT u.user_id, t.trip_id, t.price, 'TCK-10002', 'PENDING'
FROM users u
JOIN trips t ON t.travel_date = '2026-03-16' AND t.departure_time = '07:00:00'
WHERE u.email = 'komin.passenger@busticket.com'
  AND NOT EXISTS (SELECT 1 FROM bookings WHERE ticket_code = 'TCK-10002');

INSERT IGNORE INTO booking_seat (booking_id, seat_id)
SELECT b.booking_id, s.seat_id
FROM bookings b
JOIN seats s ON s.seat_number = 'A1'
JOIN trips t ON t.trip_id = b.trip_id
JOIN buses bus ON bus.bus_id = t.bus_id
WHERE b.ticket_code = 'TCK-10001'
  AND bus.bus_number = 'VIP-1001';

INSERT IGNORE INTO booking_seat (booking_id, seat_id)
SELECT b.booking_id, s.seat_id
FROM bookings b
JOIN seats s ON s.seat_number = 'B2'
JOIN trips t ON t.trip_id = b.trip_id
JOIN buses bus ON bus.bus_id = t.bus_id
WHERE b.ticket_code = 'TCK-10002'
  AND bus.bus_number = 'VIP-1002';

INSERT INTO payments (booking_id, payment_method, payment_status, paid_amount, paid_at)
SELECT b.booking_id, 'CARD', 'PAID', b.total_price, '2026-02-20 10:15:00'
FROM bookings b
WHERE b.ticket_code = 'TCK-10001'
  AND NOT EXISTS (SELECT 1 FROM payments WHERE booking_id = b.booking_id);

