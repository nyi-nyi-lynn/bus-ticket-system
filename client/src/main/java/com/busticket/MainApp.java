package com.busticket;

import com.busticket.dto.UserDTO;
import com.busticket.remote.UserRemote;
import com.busticket.rmi.RMIClient;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.List;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {

    }

    public static void main(String[] args) {
        try {
            UserRemote user = RMIClient.getUserRemote();
            List<UserDTO> users = user.getAllUsers();
            for(UserDTO userDTO : users){
                System.out.println("Name: " + userDTO.getName());
                System.out.println("Email: " + userDTO.getEmail());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
//        launch(args);
    }
}
