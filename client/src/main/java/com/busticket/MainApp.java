package com.busticket;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        Label label = new Label("Bus Ticket System Client");
        Scene scene = new Scene(label, 400, 200);
        stage.setScene(scene);
        stage.setTitle("Bus Ticket System");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
