package com.busticket.util;
import com.busticket.dto.UserDTO;

public class Session {
    private static UserDTO currentUser;
    public static void setCurrentUser(UserDTO user) { currentUser = user; }
    public static UserDTO getUser() { return currentUser; }
}