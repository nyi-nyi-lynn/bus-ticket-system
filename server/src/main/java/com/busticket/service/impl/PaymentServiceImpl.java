package com.busticket.service.impl;

import com.busticket.dao.PaymentDAO;
import com.busticket.dao.impl.PaymentDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.service.PaymentService;

public class PaymentServiceImpl implements PaymentService {

    private final PaymentDAO paymentDAO;

    public PaymentServiceImpl(){
        paymentDAO = new PaymentDAOImpl(DatabaseConnection.getConnection());
    }
}
