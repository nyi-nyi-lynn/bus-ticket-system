package com.busticket.dto;

import java.io.Serializable;
import java.time.LocalDate;

public class SalesReportDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private LocalDate fromDate;
    private LocalDate toDate;
    private long totalConfirmedBookings;
    private double totalRevenue;

    public SalesReportDTO() {
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public long getTotalConfirmedBookings() {
        return totalConfirmedBookings;
    }

    public void setTotalConfirmedBookings(long totalConfirmedBookings) {
        this.totalConfirmedBookings = totalConfirmedBookings;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
}
