package com.busticket.dao.impl;

import com.busticket.dao.BusDAO;
import com.busticket.enums.BusType;
import com.busticket.exception.DuplicateResourceException;
import com.busticket.model.Bus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BusDAOImpl implements BusDAO {
    private final Connection connection;

    public BusDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public boolean existsByBusNumber(String busNumber) {
        String sql = "SELECT 1 FROM buses WHERE bus_number = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, busNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean existsByBusNumberExceptId(String busNumber, Long busId) {
        String sql = "SELECT 1 FROM buses WHERE bus_number = ? AND bus_id <> ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, busNumber);
            ps.setLong(2, busId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Bus findById(Long busId) {
        String sql = "SELECT bus_id, bus_number, bus_name, type, total_seats, is_active FROM buses WHERE bus_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, busId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBus(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean hasTrips(Long busId) {
        String sql = "SELECT 1 FROM trips WHERE bus_id = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, busId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Bus insert(Bus bus) throws DuplicateResourceException {
        String sql = "INSERT INTO buses(bus_number, bus_name, type, total_seats, is_active) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, bus.getBusNumber());
            ps.setString(2, bus.getBusName());
            ps.setString(3, bus.getType().name());
            ps.setInt(4, bus.getTotalSeats());
            ps.setInt(5, bus.isActive() ? 1 : 0);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                return null;
            }

            Long busId = null;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    busId = keys.getLong(1);
                }
            }
            if (busId != null) {
                generateSeats(busId, bus.getTotalSeats());
                return findById(busId);
            }
            return null;
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new DuplicateResourceException("BUS_NUMBER_EXISTS");
        } catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState())) {
                throw new DuplicateResourceException("BUS_NUMBER_EXISTS");
            }
            throw new RuntimeException("Failed to insert bus.", ex);
        }
    }

    @Override
    public Bus updateRecord(Bus bus) throws DuplicateResourceException {
        String sql = "UPDATE buses SET bus_number = ?, bus_name = ?, type = ?, total_seats = ?, is_active = ? WHERE bus_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, bus.getBusNumber());
            ps.setString(2, bus.getBusName());
            ps.setString(3, bus.getType().name());
            ps.setInt(4, bus.getTotalSeats());
            ps.setInt(5, bus.isActive() ? 1 : 0);
            ps.setLong(6, bus.getBusId());

            int affected = ps.executeUpdate();
            if (affected <= 0) {
                return null;
            }
            return findById(bus.getBusId());
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new DuplicateResourceException("BUS_NUMBER_EXISTS");
        } catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState())) {
                throw new DuplicateResourceException("BUS_NUMBER_EXISTS");
            }
            throw new RuntimeException("Failed to update bus.", ex);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteSeats = connection.prepareStatement("DELETE FROM seats WHERE bus_id = ?")) {
                deleteSeats.setLong(1, id);
                deleteSeats.executeUpdate();
            }
            int affected;
            try (PreparedStatement deleteBus = connection.prepareStatement("DELETE FROM buses WHERE bus_id = ?")) {
                deleteBus.setLong(1, id);
                affected = deleteBus.executeUpdate();
            }
            connection.commit();
            return affected > 0;
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            ex.printStackTrace();
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public boolean save(Bus bus) {
        try {
            return insert(bus) != null;
        } catch (DuplicateResourceException ex) {
            return false;
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(Bus bus) {
        try {
            return updateRecord(bus) != null;
        } catch (DuplicateResourceException ex) {
            return false;
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deactivate(Long id) {
        String sql = "UPDATE buses SET is_active = 0 WHERE bus_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Bus> findAll() {
        String sql = "SELECT bus_id, bus_number, bus_name, type, total_seats, is_active FROM buses ORDER BY bus_id DESC";
        List<Bus> buses = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                buses.add(mapBus(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return buses;
    }

    private Bus mapBus(ResultSet rs) throws SQLException {
        Bus bus = new Bus();
        bus.setBusId(rs.getLong("bus_id"));
        bus.setBusNumber(rs.getString("bus_number"));
        bus.setBusName(rs.getString("bus_name"));
        String typeValue = rs.getString("type");
        try {
            bus.setType(BusType.valueOf(typeValue));
        } catch (IllegalArgumentException ex) {
            bus.setType(BusType.NORMAL);
        }
        bus.setTotalSeats(rs.getInt("total_seats"));
        bus.setActive(rs.getInt("is_active") == 1);
        return bus;
    }

    private void generateSeats(Long busId, int totalSeats) {
        String sql = "INSERT INTO seats(bus_id, seat_number) VALUES(?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 1; i <= totalSeats; i++) {
                ps.setLong(1, busId);
                ps.setString(2, "S" + i);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
