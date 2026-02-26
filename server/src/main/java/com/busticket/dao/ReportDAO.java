package com.busticket.dao;

import com.busticket.dto.BookingRowDTO;
import com.busticket.dto.CustomerActivityRowDTO;
import com.busticket.dto.ReportFilterDTO;
import com.busticket.dto.RevenueRowDTO;
import com.busticket.dto.RoutePopularityRowDTO;
import com.busticket.dto.TripPerformanceRowDTO;

import java.util.List;

public interface ReportDAO {
    List<RevenueRowDTO> fetchRevenueSummary(ReportFilterDTO filter);

    List<BookingRowDTO> fetchBookingSummary(ReportFilterDTO filter);

    List<TripPerformanceRowDTO> fetchTripPerformance(ReportFilterDTO filter);

    List<RoutePopularityRowDTO> fetchRoutePopularity(ReportFilterDTO filter);

    List<CustomerActivityRowDTO> fetchCustomerActivity(ReportFilterDTO filter);
}
