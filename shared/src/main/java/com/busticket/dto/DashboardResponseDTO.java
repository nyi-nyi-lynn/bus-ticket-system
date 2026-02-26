package com.busticket.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardResponseDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<KpiDTO> kpis = new ArrayList<>();
    private List<ChartPointDTO> revenueTrend = new ArrayList<>();
    private Map<String, Long> bookingStatusSummary = new LinkedHashMap<>();
    private List<RoutePopularityRowDTO> topRoutes = new ArrayList<>();
    private List<BookingDTO> recentBookings = new ArrayList<>();
    private List<String> alerts = new ArrayList<>();
    private LocalDateTime generatedAt;

    public List<KpiDTO> getKpis() {
        return kpis;
    }

    public void setKpis(List<KpiDTO> kpis) {
        this.kpis = kpis == null ? new ArrayList<>() : new ArrayList<>(kpis);
    }

    public List<ChartPointDTO> getRevenueTrend() {
        return revenueTrend;
    }

    public void setRevenueTrend(List<ChartPointDTO> revenueTrend) {
        this.revenueTrend = revenueTrend == null ? new ArrayList<>() : new ArrayList<>(revenueTrend);
    }

    public Map<String, Long> getBookingStatusSummary() {
        return bookingStatusSummary;
    }

    public void setBookingStatusSummary(Map<String, Long> bookingStatusSummary) {
        this.bookingStatusSummary = bookingStatusSummary == null ? new LinkedHashMap<>() : new LinkedHashMap<>(bookingStatusSummary);
    }

    public List<RoutePopularityRowDTO> getTopRoutes() {
        return topRoutes;
    }

    public void setTopRoutes(List<RoutePopularityRowDTO> topRoutes) {
        this.topRoutes = topRoutes == null ? new ArrayList<>() : new ArrayList<>(topRoutes);
    }

    public List<BookingDTO> getRecentBookings() {
        return recentBookings;
    }

    public void setRecentBookings(List<BookingDTO> recentBookings) {
        this.recentBookings = recentBookings == null ? new ArrayList<>() : new ArrayList<>(recentBookings);
    }

    public List<String> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<String> alerts) {
        this.alerts = alerts == null ? new ArrayList<>() : new ArrayList<>(alerts);
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
