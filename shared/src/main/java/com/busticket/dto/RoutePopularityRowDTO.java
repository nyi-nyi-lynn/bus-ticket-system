package com.busticket.dto;

import java.io.Serial;
import java.io.Serializable;

public class RoutePopularityRowDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long routeId;
    private String routeLabel;
    private long totalBookings;
    private double totalRevenue;

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public String getRouteLabel() {
        return routeLabel;
    }

    public void setRouteLabel(String routeLabel) {
        this.routeLabel = routeLabel;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
}
