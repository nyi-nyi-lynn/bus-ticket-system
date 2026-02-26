package com.busticket.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PassengerDashboardDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private UserSummaryDTO userSummary;
    private BookingSummaryDTO bookingSummary;
    private UpcomingTripDTO upcomingTrip;
    private List<RecentBookingDTO> recentBookings;
    private List<String> notifications;
    private LocalDateTime generatedAt;

    public UserSummaryDTO getUserSummary() {
        return userSummary;
    }

    public void setUserSummary(UserSummaryDTO userSummary) {
        this.userSummary = userSummary;
    }

    public BookingSummaryDTO getBookingSummary() {
        return bookingSummary;
    }

    public void setBookingSummary(BookingSummaryDTO bookingSummary) {
        this.bookingSummary = bookingSummary;
    }

    public UpcomingTripDTO getUpcomingTrip() {
        return upcomingTrip;
    }

    public void setUpcomingTrip(UpcomingTripDTO upcomingTrip) {
        this.upcomingTrip = upcomingTrip;
    }

    public List<RecentBookingDTO> getRecentBookings() {
        return recentBookings == null ? new ArrayList<>() : new ArrayList<>(recentBookings);
    }

    public void setRecentBookings(List<RecentBookingDTO> recentBookings) {
        this.recentBookings = recentBookings == null ? new ArrayList<>() : new ArrayList<>(recentBookings);
    }

    public List<String> getNotifications() {
        return notifications == null ? new ArrayList<>() : new ArrayList<>(notifications);
    }

    public void setNotifications(List<String> notifications) {
        this.notifications = notifications == null ? new ArrayList<>() : new ArrayList<>(notifications);
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
