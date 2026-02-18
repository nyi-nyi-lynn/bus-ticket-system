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
            UserDTO loginUser = user.login("nyinyi@gmail.com","password");
            if(loginUser != null){
                System.out.println("Login successful");
                System.out.println(loginUser.getName());
            }else {
                System.out.println("Login failed");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
//        launch(args);
    }
}
