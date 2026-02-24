package com.busticket.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class BookingRequestDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // ADDED
    private Long userId;
    // ADDED
    private Long tripId;
    // MODIFIED
    private List<String> seatNumbers;

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
