package com.busticket.remote;

import com.busticket.dto.ReportFilterDTO;
import com.busticket.dto.ReportResponseDTO;
import com.busticket.exception.ValidationException;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ReportRemote extends Remote {
    ReportResponseDTO getRevenueReport(ReportFilterDTO filter) throws ValidationException, RemoteException;

    ReportResponseDTO getBookingReport(ReportFilterDTO filter) throws ValidationException, RemoteException;

    ReportResponseDTO getTripPerformanceReport(ReportFilterDTO filter) throws ValidationException, RemoteException;

    ReportResponseDTO getRoutePopularityReport(ReportFilterDTO filter) throws ValidationException, RemoteException;

    ReportResponseDTO getCustomerActivityReport(ReportFilterDTO filter) throws ValidationException, RemoteException;
}
