package com.busticket.dto;

import java.io.Serial;
import java.io.Serializable;

public class BookingResponseDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // ADDED
    private Long bookingId;
    // ADDED
    private String ticketCode;
    // ADDED
    private Double totalAmount;
    // ADDED
    private String status;
    private String paymentStatus;

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}
