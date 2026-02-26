package com.busticket.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

public class TripPerformanceRowDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long tripId;
    private String routeLabel;
    private String busNumber;
    private LocalDate travelDate;
    private int totalSeats;
    private long soldSeats;
    private double occupancyRate;
    private double totalRevenue;

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getRouteLabel() {
        return routeLabel;
    }

    public void setRouteLabel(String routeLabel) {
        this.routeLabel = routeLabel;
    }

    public String getBusNumber() {
        return busNumber;
    }

    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
    }

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public long getSoldSeats() {
        return soldSeats;
    }

    public void setSoldSeats(long soldSeats) {
        this.soldSeats = soldSeats;
    }

    public double getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(double occupancyRate) {
        this.occupancyRate = occupancyRate;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
}
