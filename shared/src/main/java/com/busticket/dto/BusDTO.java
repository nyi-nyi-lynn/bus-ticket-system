package com.busticket.dto;

import java.io.Serializable;

public class BusDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long busId;
    private String busNumber;
    private String busName;
    private String type;
    private String status;
    private int totalSeats;

    public BusDTO() {
    }

    public BusDTO(Long busId, String busNumber, String busName, String type, String status, int totalSeats) {
        this.busId = busId;
        this.busNumber = busNumber;
        this.busName = busName;
        this.type = type;
        this.status = status;
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

    public String getBusName() {
        return busName;
    }

    public void setBusName(String busName) {
        this.busName = busName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }
}
