package com.busticket.service;

import com.busticket.dto.DashboardResponseDTO;
import com.busticket.exception.ValidationException;

public interface DashboardService {
    DashboardResponseDTO getDashboardSummary(Long requestedByUserId) throws ValidationException;
}
