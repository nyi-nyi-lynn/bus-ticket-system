package com.busticket.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import java.io.IOException;

public class SceneManager {

    private static Stage stage;

    public static void switchScene(ActionEvent event, String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/busticket/view/" + fxmlFile));
            Parent root = loader.load();

            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen(); // စာမျက်နှာကို screen အလယ်ပို့ပေးခြင်း
            stage.show();

        } catch (IOException e) {
            System.err.println("Error: Could not load FXML file -> " + fxmlFile);
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Error: FXML file path is incorrect!");
            e.printStackTrace();
        }
    }
}