package com.busticket.dao;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.BookingRowDTO;
import com.busticket.dto.RevenueRowDTO;
import com.busticket.dto.RoutePopularityRowDTO;
import com.busticket.dto.TripPerformanceRowDTO;

import java.time.LocalDate;
import java.util.List;

public interface DashboardDAO {
    double fetchRevenueToday();

    double fetchRevenueThisMonth();

    long fetchBookingsToday();

    long fetchActiveTrips();

    long fetchActiveBuses();

    long fetchInactiveBuses();

    long fetchTotalUsers();

    List<RevenueRowDTO> fetchRevenueTrend(LocalDate fromDate, LocalDate toDate);

    List<BookingRowDTO> fetchBookingStatusSummary(LocalDate fromDate, LocalDate toDate);

    List<RoutePopularityRowDTO> fetchTopRoutes(LocalDate fromDate, LocalDate toDate, int limit);

    List<BookingDTO> fetchRecentBookings(int limit);

    List<TripPerformanceRowDTO> fetchLowOccupancyTrips(LocalDate fromDate, LocalDate toDate, double threshold, int limit);
}
