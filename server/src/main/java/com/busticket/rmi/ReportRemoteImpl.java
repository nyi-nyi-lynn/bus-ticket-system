package com.busticket.rmi;

import com.busticket.dto.ReportFilterDTO;
import com.busticket.dto.ReportResponseDTO;
import com.busticket.exception.ValidationException;
import com.busticket.remote.ReportRemote;
import com.busticket.service.ReportService;
import com.busticket.service.impl.ReportServiceImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ReportRemoteImpl extends UnicastRemoteObject implements ReportRemote {
    public ReportRemoteImpl() throws RemoteException {
    }

    private ReportService reportService() {
        return new ReportServiceImpl();
    }

    @Override
    public ReportResponseDTO getRevenueReport(ReportFilterDTO filter) throws ValidationException, RemoteException {
        return reportService().getRevenueReport(filter);
    }

    @Override
    public ReportResponseDTO getBookingReport(ReportFilterDTO filter) throws ValidationException, RemoteException {
        return reportService().getBookingReport(filter);
    }

    @Override
    public ReportResponseDTO getTripPerformanceReport(ReportFilterDTO filter) throws ValidationException, RemoteException {
        return reportService().getTripPerformanceReport(filter);
    }

    @Override
    public ReportResponseDTO getRoutePopularityReport(ReportFilterDTO filter) throws ValidationException, RemoteException {
        return reportService().getRoutePopularityReport(filter);
    }

    @Override
    public ReportResponseDTO getCustomerActivityReport(ReportFilterDTO filter) throws ValidationException, RemoteException {
        return reportService().getCustomerActivityReport(filter);
    }
}
