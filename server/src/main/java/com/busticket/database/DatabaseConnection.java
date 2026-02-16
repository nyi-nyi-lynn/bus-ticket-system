package com.busticket.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/bus_ticket";
    private static final String USER = "root";
    private static final String PASSWORD = "rootpass";

    private static Connection connection;

    public static Connection getConnection(){
        try {
            if(connection == null || connection.isClosed()){
                connection = DriverManager.getConnection(URL,USER,PASSWORD);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }

        return connection;
    }
}
