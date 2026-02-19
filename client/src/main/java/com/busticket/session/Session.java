package com.busticket.session;

import com.busticket.dto.UserDTO;
import com.busticket.enums.Role;

public final class Session {
    private static UserDTO currentUser;
    private static Role role = Role.PASSENGER;
    private static boolean guest = true;

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
