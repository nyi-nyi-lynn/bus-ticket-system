package com.busticket.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BookingRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long tripId;
    private List<String> seatNumbers = new ArrayList<>();

    public BookingRequestDTO() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public List<String> getSeatNumbers() {
        return seatNumbers;
    }

    public void setSeatNumbers(List<String> seatNumbers) {
        this.seatNumbers = seatNumbers;
    }
}
