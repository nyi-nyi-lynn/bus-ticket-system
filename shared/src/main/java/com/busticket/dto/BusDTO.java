package com.busticket.dto;

import java.io.Serializable;

public class BusDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long busId;
    private String busNumber;
    private String type;
    private int totalSeats;

    public BusDTO() {
    }

    public BusDTO(Long busId, String busNumber, String type, int totalSeats) {
        this.busId = busId;
        this.busNumber = busNumber;
        this.type = type;
        this.totalSeats = totalSeats;
    }

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }
}
