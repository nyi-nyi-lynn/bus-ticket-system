package com.busticket.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BookingSummaryDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private TripDTO trip;
    private List<SeatDTO> selectedSeats;
    private UserDTO currentUser;
    private String paymentMethod;
    private double pricePerSeat;
    private double totalPrice;

    public TripDTO getTrip() {
        return trip;
    }

    public void setTrip(TripDTO trip) {
        this.trip = trip;
    }

    public List<SeatDTO> getSelectedSeats() {
        return selectedSeats;
    }

    public void setSelectedSeats(List<SeatDTO> selectedSeats) {
        this.selectedSeats = selectedSeats == null ? new ArrayList<>() : new ArrayList<>(selectedSeats);
    }

    public UserDTO getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserDTO currentUser) {
        this.currentUser = currentUser;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getPricePerSeat() {
        return pricePerSeat;
    }

    public void setPricePerSeat(double pricePerSeat) {
        this.pricePerSeat = pricePerSeat;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
