package com.busticket.dto;

import java.io.Serializable;

public class SeatDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long seatId;
    private Long busId;
    private String seatNumber;

    public SeatDTO() {
    }

    public SeatDTO(Long seatId, Long busId, String seatNumber) {
        this.seatId = seatId;
        this.busId = busId;
        this.seatNumber = seatNumber;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public Long getBusId() {
        return busId;
    }

    public void setBusId(Long busId) {
        this.busId = busId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }
}
