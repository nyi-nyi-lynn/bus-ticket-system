package com.busticket.service;

import com.busticket.dto.ReportFilterDTO;
import com.busticket.dto.ReportResponseDTO;
import com.busticket.exception.ValidationException;

public interface ReportService {
    ReportResponseDTO getRevenueReport(ReportFilterDTO filter) throws ValidationException;

    ReportResponseDTO getBookingReport(ReportFilterDTO filter) throws ValidationException;

    ReportResponseDTO getTripPerformanceReport(ReportFilterDTO filter) throws ValidationException;

    ReportResponseDTO getRoutePopularityReport(ReportFilterDTO filter) throws ValidationException;

    ReportResponseDTO getCustomerActivityReport(ReportFilterDTO filter) throws ValidationException;
}
