package com.busticket.dao.impl;

import com.busticket.dao.BookingDAO;
import com.busticket.enums.BookingStatus;
import com.busticket.model.Booking;

import java.sql.Connection;
import java.util.List;

public class BookingDAOImpl implements BookingDAO {

    private final Connection connection ;

    public BookingDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Long createBooking( Booking booking) {
        return 0L;
    }

    @Override
    public void insertBookingSeats(Long bookingId, List<Long> seatIds) {

    }

    @Override
    public List<Long> findBookedSeats(Long tripId) {
        return List.of();
    }

    @Override
    public boolean updateStatus(Long bookingId, BookingStatus status) {
        return false;
    }
}
