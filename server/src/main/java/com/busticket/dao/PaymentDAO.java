package com.busticket.dao;

import com.busticket.model.Payment;

public interface PaymentDAO {
    Long create(Payment payment);
}
