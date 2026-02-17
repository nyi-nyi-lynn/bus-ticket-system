package com.busticket.util;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public final class Navigator {
    private Navigator() {
    }

    public static void switchScene(Stage stage, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(Navigator.class.getResource(fxmlPath));
            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root);
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            playFade(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML: " + fxmlPath, e);
        }
    }

    private static void playFade(Parent root) {
        FadeTransition ft = new FadeTransition(Duration.millis(180), root);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }
}
