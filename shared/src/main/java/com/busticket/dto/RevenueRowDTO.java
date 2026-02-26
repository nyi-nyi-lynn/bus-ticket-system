package com.busticket.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

public class RevenueRowDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDate date;
    private long totalBookings;
    private double totalRevenue;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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
