package com.busticket.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BookingRequestDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long tripId;
    private List<String> seatNumbers;
    private List<Long> seatIds;
    private String paymentMethod;

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
        this.seatNumbers = seatNumbers == null ? new ArrayList<>() : new ArrayList<>(seatNumbers);
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds == null ? new ArrayList<>() : new ArrayList<>(seatIds);
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
