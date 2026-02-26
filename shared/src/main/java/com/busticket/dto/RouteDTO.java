package com.busticket.dto;

import java.io.Serializable;

public class RouteDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long routeId;
    private String originCity;
    private String destinationCity;
    private double distanceKm;
    private String estimatedDuration;
    private String status;

    public RouteDTO() {
    }

    public RouteDTO(Long routeId, String originCity, String destinationCity, double distanceKm, String estimatedDuration) {
        this.routeId = routeId;
        this.originCity = originCity;
        this.destinationCity = destinationCity;
        this.distanceKm = distanceKm;
        this.estimatedDuration = estimatedDuration;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public String getOriginCity() {
        return originCity;
    }

    public void setOriginCity(String originCity) {
        this.originCity = originCity;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public void setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(String estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
