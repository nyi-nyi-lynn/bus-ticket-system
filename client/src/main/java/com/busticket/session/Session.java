package com.busticket.session;

import com.busticket.dto.UserDTO;
import com.busticket.dto.TripDTO;
import com.busticket.dto.SeatDTO;
import com.busticket.enums.Role;

import java.util.ArrayList;
import java.util.List;

public final class Session {
    private static UserDTO currentUser;
    private static Role role = Role.PASSENGER;
    private static boolean guest = true;
    private static TripDTO pendingTrip;
    private static List<String> pendingSeatNumbers = new ArrayList<>();
    private static List<SeatDTO> pendingSeats = new ArrayList<>();
    private static Long currentBookingId;
    private static Long currentBookingUserId;
    private static String currentTicketCode;
    private static Double currentBookingAmount;

    private Session() {
    }

    public static void login(UserDTO user) {
        currentUser = user;
        guest = false;
        role = resolveRole(user != null ? user.getRole() : null);
    }

    public static void loginAsGuest() {
        currentUser = null;
        guest = true;
        role = Role.PASSENGER;
    }

    public static void clear() {
        currentUser = null;
        guest = true;
        role = Role.PASSENGER;
        clearPendingSelection();
        clearBookingContext();
    }

    public static UserDTO getCurrentUser() {
        return currentUser;
    }

    public static Role getRole() {
        return role;
    }

    public static boolean isGuest() {
        return guest;
    }

    // ADDED
    public static void setPendingSelection(TripDTO trip, List<String> seatNumbers) {
        pendingTrip = trip;
        pendingSeatNumbers = seatNumbers == null ? new ArrayList<>() : new ArrayList<>(seatNumbers);
        pendingSeats = new ArrayList<>();
    }

    public static void setPendingSelection(TripDTO trip, List<SeatDTO> seats, List<String> seatNumbers) {
        pendingTrip = trip;
        pendingSeats = seats == null ? new ArrayList<>() : new ArrayList<>(seats);
        pendingSeatNumbers = seatNumbers == null ? new ArrayList<>() : new ArrayList<>(seatNumbers);
    }

    // ADDED
    public static TripDTO getPendingTrip() {
        return pendingTrip;
    }

    // ADDED
    public static List<String> getPendingSeatNumbers() {
        return new ArrayList<>(pendingSeatNumbers);
    }

    public static List<SeatDTO> getPendingSeats() {
        return new ArrayList<>(pendingSeats);
    }

    // ADDED
    public static void clearPendingSelection() {
        pendingTrip = null;
        pendingSeatNumbers = new ArrayList<>();
        pendingSeats = new ArrayList<>();
    }

    // ADDED
    public static void setCurrentBookingContext(Long bookingId, String ticketCode, Double totalAmount) {
        setCurrentBookingContext(bookingId, null, ticketCode, totalAmount);
    }

    public static void setCurrentBookingContext(Long bookingId, Long bookingUserId, String ticketCode, Double totalAmount) {
        currentBookingId = bookingId;
        currentBookingUserId = bookingUserId;
        currentTicketCode = ticketCode;
        currentBookingAmount = totalAmount;
    }

    // ADDED
    public static Long getCurrentBookingId() {
        return currentBookingId;
    }

    public static Long getCurrentBookingUserId() {
        return currentBookingUserId;
    }

    // ADDED
    public static String getCurrentTicketCode() {
        return currentTicketCode;
    }

    // ADDED
    public static Double getCurrentBookingAmount() {
        return currentBookingAmount;
    }

    // ADDED
    public static void clearBookingContext() {
        currentBookingId = null;
        currentBookingUserId = null;
        currentTicketCode = null;
        currentBookingAmount = null;
    }

    private static Role resolveRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return Role.PASSENGER;
        }
        try {
            return Role.valueOf(roleValue.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Role.PASSENGER;
        }
    }
}
