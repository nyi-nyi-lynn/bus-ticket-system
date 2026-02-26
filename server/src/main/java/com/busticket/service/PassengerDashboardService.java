package com.busticket.service;

import com.busticket.dto.PassengerDashboardDTO;
import com.busticket.exception.UnauthorizedException;
import com.busticket.exception.ValidationException;

public interface PassengerDashboardService {
    PassengerDashboardDTO getDashboard(Long userId) throws ValidationException, UnauthorizedException;
}
