package com.busticket.dao.impl;

import com.busticket.dao.PaymentDAO;

import java.sql.Connection;

public class PaymentDAOImpl implements PaymentDAO {

    private final Connection connection ;

    public PaymentDAOImpl(Connection connection){
        this.connection = connection;
    }
}
