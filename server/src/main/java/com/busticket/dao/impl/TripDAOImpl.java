package com.busticket.dao.impl;

import com.busticket.dao.TripDAO;
import com.busticket.enums.TripStatus;
import com.busticket.model.Trip;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TripDAOImpl implements TripDAO {
    private final Connection connection;

    public TripDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public int autoCloseOverdueTrips() {
        String sql = """
            UPDATE trips
            SET status = 'CLOSED'
            WHERE status = 'OPEN'
              AND TIMESTAMP(travel_date, departure_time) <= (NOW() + INTERVAL 1 HOUR)
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }


    @Override
    public boolean save(Trip trip) {
        String sql = """
            INSERT INTO trips(bus_id, route_id, travel_date, departure_time, arrival_time, price, status)
            VALUES(?,?,?,?,?,?,?)
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, trip.getBusId());
            ps.setLong(2, trip.getRouteId());
            ps.setDate(3, Date.valueOf(trip.getTravelDate()));
            ps.setTime(4, Time.valueOf(trip.getDepartureTime()));
            ps.setTime(5, Time.valueOf(trip.getArrivalTime()));
            ps.setDouble(6, trip.getPrice());
            ps.setString(7, trip.getStatus() == null ? TripStatus.OPEN.name() : trip.getStatus().name());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean update(Trip trip) {
        String sql = """
            UPDATE trips
            SET bus_id = ?, route_id = ?, travel_date = ?, departure_time = ?, arrival_time = ?, price = ?, status = ?
            WHERE trip_id = ?
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, trip.getBusId());
            ps.setLong(2, trip.getRouteId());
            ps.setDate(3, Date.valueOf(trip.getTravelDate()));
            ps.setTime(4, Time.valueOf(trip.getDepartureTime()));
            ps.setTime(5, Time.valueOf(trip.getArrivalTime()));
            ps.setDouble(6, trip.getPrice());
            ps.setString(7, trip.getStatus() == null ? TripStatus.OPEN.name() : trip.getStatus().name());
            ps.setLong(8, trip.getTripId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete(Long id) {
        String sql = "UPDATE trips SET status = ? WHERE trip_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, TripStatus.CLOSED.name());
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Trip> findAll() {
        autoCloseOverdueTrips();
        String sql = """
            SELECT t.trip_id, t.bus_id, t.route_id, t.travel_date, t.departure_time, t.arrival_time, t.price, t.status,
                   b.bus_number, b.total_seats, r.origin_city, r.destination_city,
                   (b.total_seats - IFNULL(bs.booked_count, 0)) AS available_seats
            FROM trips t
            JOIN buses b ON t.bus_id = b.bus_id
            JOIN routes r ON t.route_id = r.route_id
            LEFT JOIN (
                SELECT bk.trip_id, COUNT(*) AS booked_count
                FROM bookings bk
                JOIN booking_seat bks ON bk.booking_id = bks.booking_id
                WHERE bk.status = 'CONFIRMED'
                   OR (bk.status = 'PENDING' AND bk.created_at >= (NOW() - INTERVAL 15 MINUTE))
                GROUP BY bk.trip_id
            ) bs ON bs.trip_id = t.trip_id
            ORDER BY t.trip_id DESC
        """;
        List<Trip> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapTrip(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Trip> search(String origin, String destination, LocalDate date) {
        autoCloseOverdueTrips();
        String sql = """
            SELECT t.trip_id, t.bus_id, t.route_id, t.travel_date, t.departure_time, t.arrival_time, t.price, t.status,
                   b.bus_number, b.total_seats, r.origin_city, r.destination_city,
                   (b.total_seats - IFNULL(bs.booked_count, 0)) AS available_seats
            FROM trips t
            JOIN buses b ON t.bus_id = b.bus_id
            JOIN routes r ON t.route_id = r.route_id
            LEFT JOIN (
                SELECT bk.trip_id, COUNT(*) AS booked_count
                FROM bookings bk
                JOIN booking_seat bks ON bk.booking_id = bks.booking_id
                WHERE bk.status = 'CONFIRMED'
                   OR (bk.status = 'PENDING' AND bk.created_at >= (NOW() - INTERVAL 15 MINUTE))
                GROUP BY bk.trip_id
            ) bs ON bs.trip_id = t.trip_id
            WHERE r.origin_city = ?
              AND r.destination_city = ?
              AND t.travel_date = ?
              AND t.status = 'OPEN'
        """;

        List<Trip> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, origin);
            ps.setString(2, destination);
            ps.setDate(3, Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTrip(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    private Trip mapTrip(ResultSet rs) throws SQLException {
        Trip trip = new Trip();
        trip.setTripId(rs.getLong("trip_id"));
        trip.setBusId(rs.getLong("bus_id"));
        trip.setRouteId(rs.getLong("route_id"));
        trip.setTravelDate(rs.getDate("travel_date").toLocalDate());
        trip.setDepartureTime(rs.getTime("departure_time").toLocalTime());
        trip.setArrivalTime(rs.getTime("arrival_time").toLocalTime());
        trip.setPrice(rs.getDouble("price"));
        trip.setStatus(TripStatus.valueOf(rs.getString("status")));
        trip.setBusNumber(rs.getString("bus_number"));
        trip.setOriginCity(rs.getString("origin_city"));
        trip.setDestinationCity(rs.getString("destination_city"));
        trip.setTotalSeats(rs.getInt("total_seats"));
        trip.setAvailableSeats(rs.getInt("available_seats"));
        return trip;
    }
}
