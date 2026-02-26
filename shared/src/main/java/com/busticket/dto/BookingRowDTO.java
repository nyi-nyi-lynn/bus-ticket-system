package com.busticket.dto;

import java.io.Serial;
import java.io.Serializable;

public class BookingRowDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String bookingStatus;
    private long totalBookings;
    private double totalRevenue;

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
}
