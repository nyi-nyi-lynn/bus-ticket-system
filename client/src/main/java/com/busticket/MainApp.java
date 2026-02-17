package com.busticket;

import atlantafx.base.theme.PrimerLight;
import com.busticket.util.Navigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        stage.setTitle("Bus Ticket System");
        stage.setWidth(1200);
        stage.setHeight(780);
        stage.setMinWidth(1060);
        stage.setMinHeight(700);
        Navigator.switchScene(stage, "/com/busticket/view/auth/login.fxml");
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
