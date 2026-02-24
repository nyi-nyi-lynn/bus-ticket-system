package com.busticket.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = getEnvOrDefault("BTS_DB_URL", "jdbc:mysql://localhost:3306/bus_ticket");
    private static final String USER = getEnvOrDefault("BTS_DB_USER", "root");
    private static final String PASSWORD = getEnvOrDefault("BTS_DB_PASSWORD", "rootpass");

    public static Connection getConnection(){
        try {
            // FIXED: return a fresh JDBC connection per call to avoid cross-thread sharing.
            return DriverManager.getConnection(URL,USER,PASSWORD);
        }catch (SQLException e){
            e.printStackTrace();
            return null;
        }
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
