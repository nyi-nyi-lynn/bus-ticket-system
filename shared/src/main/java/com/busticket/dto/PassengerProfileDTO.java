package com.busticket.dto;

import java.io.Serial;
import java.io.Serializable;

public class PassengerProfileDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String fullName;
    private String username;
    private String email;
    private String role;
    private String phone;
    private String dateOfBirth;
    private String memberSince;
    private long totalBookings;
    private long completedTrips;
    private long cancelledTrips;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getMemberSince() {
        return memberSince;
    }

    public void setMemberSince(String memberSince) {
        this.memberSince = memberSince;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public long getCompletedTrips() {
        return completedTrips;
    }

    public void setCompletedTrips(long completedTrips) {
        this.completedTrips = completedTrips;
    }

    public long getCancelledTrips() {
        return cancelledTrips;
    }

    public void setCancelledTrips(long cancelledTrips) {
        this.cancelledTrips = cancelledTrips;
    }
}
