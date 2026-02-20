package com.busticket;

import com.busticket.dto.TripDTO;
import com.busticket.remote.TripRemote;
import com.busticket.rmi.RMIClient;
import javafx.application.Application;
import javafx.stage.Stage;

import com.busticket.util.SceneSwitcher;

import java.util.List;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        SceneSwitcher.init(stage);
        SceneSwitcher.showAuth("/com/busticket/view/auth/LoginView.fxml");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
