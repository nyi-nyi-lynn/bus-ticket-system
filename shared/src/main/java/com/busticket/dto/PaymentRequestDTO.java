package com.busticket.dto;

import java.io.Serial;
import java.io.Serializable;

public class PaymentRequestDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long bookingId;
    private Double amount;
    private String paymentMethod;

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
