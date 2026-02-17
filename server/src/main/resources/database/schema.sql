CREATE DATABASE IF NOT EXISTS bus_ticket;
USE bus_ticket;

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(190) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
    role ENUM('ADMIN', 'STAFF', 'PASSENGER') NOT NULL DEFAULT 'PASSENGER',
    status ENUM('ACTIVE', 'BLOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS buses (
    bus_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bus_number VARCHAR(80) NOT NULL UNIQUE,
    type ENUM('VIP', 'NORMAL') NOT NULL DEFAULT 'NORMAL',
    total_seats INT NOT NULL CHECK (total_seats > 0)
);

CREATE TABLE IF NOT EXISTS routes (
    route_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    origin_city VARCHAR(120) NOT NULL,
    destination_city VARCHAR(120) NOT NULL,
    distance_km DECIMAL(10,2) NOT NULL CHECK (distance_km >= 0),
    estimated_duration VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS trips (
    trip_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bus_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    travel_date DATE NOT NULL,
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    price DECIMAL(12,2) NOT NULL CHECK (price > 0),
    status ENUM('OPEN', 'CLOSED') NOT NULL DEFAULT 'OPEN',
    UNIQUE KEY uk_trip_schedule (bus_id, travel_date, departure_time),
    CONSTRAINT fk_trips_bus FOREIGN KEY (bus_id) REFERENCES buses(bus_id),
    CONSTRAINT fk_trips_route FOREIGN KEY (route_id) REFERENCES routes(route_id)
);

CREATE TABLE IF NOT EXISTS seats (
    seat_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bus_id BIGINT NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    UNIQUE KEY uk_bus_seat (bus_id, seat_number),
    CONSTRAINT fk_seats_bus FOREIGN KEY (bus_id) REFERENCES buses(bus_id)
);

CREATE TABLE IF NOT EXISTS bookings (
    booking_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    trip_id BIGINT NOT NULL,
    booking_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_price DECIMAL(12,2) NOT NULL CHECK (total_price > 0),
    ticket_code VARCHAR(120) NOT NULL UNIQUE,
    status ENUM('PENDING', 'CONFIRMED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_bookings_trip FOREIGN KEY (trip_id) REFERENCES trips(trip_id)
);

CREATE TABLE IF NOT EXISTS booking_seat (
    booking_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    PRIMARY KEY (booking_id, seat_id),
    CONSTRAINT fk_booking_seat_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id),
    CONSTRAINT fk_booking_seat_seat FOREIGN KEY (seat_id) REFERENCES seats(seat_id)
);

CREATE TABLE IF NOT EXISTS payments (
    payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL UNIQUE,
    payment_method ENUM('CARD', 'MOBILE_BANKING') NOT NULL,
    payment_status ENUM('PENDING', 'PAID', 'FAILED') NOT NULL DEFAULT 'PENDING',
    paid_amount DECIMAL(12,2) NOT NULL CHECK (paid_amount >= 0),
    paid_at DATETIME,
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id)
);

CREATE TABLE IF NOT EXISTS ticket_validations (
    validation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL UNIQUE,
    staff_user_id BIGINT NOT NULL,
    validated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_validation_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id),
    CONSTRAINT fk_validation_staff FOREIGN KEY (staff_user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_trips_travel_date ON trips (travel_date);
CREATE INDEX idx_trips_departure_time ON trips (departure_time);
CREATE INDEX idx_trips_route_id ON trips (route_id);
CREATE INDEX idx_trips_bus_id ON trips (bus_id);
CREATE INDEX idx_routes_origin_dest ON routes (origin_city, destination_city);
CREATE INDEX idx_bookings_user_id ON bookings (user_id);
CREATE INDEX idx_bookings_trip_id ON bookings (trip_id);
CREATE INDEX idx_booking_seat_seat_id ON booking_seat (seat_id);

INSERT INTO users(name, email, password, phone, role, status)
SELECT 'System Admin', 'admin@busticket.com', SHA2('admin123', 256), '0000000000', 'ADMIN', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@busticket.com');

INSERT INTO users(name, email, password, phone, role, status)
SELECT 'System Staff', 'staff@busticket.com', SHA2('staff123', 256), '0000000001', 'STAFF', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'staff@busticket.com');
