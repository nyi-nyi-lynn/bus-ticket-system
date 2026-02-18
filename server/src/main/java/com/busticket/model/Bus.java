package com.busticket.model;

import com.busticket.enums.BusType;

public class Bus {
    private Long busId;
    private String busNumber;
    private BusType type;
    private int totalSeats;

    public Long getBusId() {
        return busId;
    }

    public void setBusId(Long busId) {
        this.busId = busId;
    }

    public String getBusNumber() {
        return busNumber;
    }

    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
    }

    public BusType getType() {
        return type;
    }

    public void setType(BusType type) {
        this.type = type;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }
}
