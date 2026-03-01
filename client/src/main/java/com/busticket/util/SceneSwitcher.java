package com.busticket.util;

import com.busticket.controller.shell.AppShellController;
import com.busticket.controller.passenger.SeatSelectionController;
import com.busticket.dto.TripDTO;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public final class SceneSwitcher {
    private static Stage primaryStage;
    private static AppShellController appShellController;

    private SceneSwitcher() {
    }

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static void showAuth(String fxmlPath) {
        Parent root = load(fxmlPath);
        if (root == null) {
            return;
        }
        Scene scene = new Scene(root, 1280, 720);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Bus Ticket System");
        playFade(root);
        primaryStage.show();
    }

    public static void showAppShell(String initialContentFxml) {
        FXMLLoader loader = new FXMLLoader(SceneSwitcher.class.getResource("/com/busticket/view/shell/AppShell.fxml"));
        try {
            Parent shellRoot = loader.load();
            appShellController = loader.getController();
            Scene scene = new Scene(shellRoot, 1280, 720);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Bus Ticket System");
            primaryStage.show();
            switchContent(initialContentFxml);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void switchContent(String fxmlPath) {
        if (appShellController == null) {
            return;
        }
        Parent content = load(fxmlPath);
        if (content == null) {
            return;
        }
        appShellController.setContent(content);
        playFade(content);
    }

    public static void switchToBookingSummary() {
        switchContent("/com/busticket/view/passenger/BookingSummary.fxml");
    }

    public static void switchToSeatSelection(TripDTO trip) {
        if (appShellController == null || trip == null) {
            return;
        }
        FXMLLoader loader = new FXMLLoader(SceneSwitcher.class.getResource("/com/busticket/view/passenger/SeatSelectionView.fxml"));
        try {
            Parent content = loader.load();
            SeatSelectionController controller = loader.getController();
            controller.setTripData(trip);
            appShellController.setContent(content);
            playFade(content);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void resetToAuth(String fxmlPath) {
        appShellController = null;
        showAuth(fxmlPath);
    }

    private static Parent load(String fxmlPath) {
        try {
            return FXMLLoader.load(SceneSwitcher.class.getResource(fxmlPath));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void playFade(Node node) {
        FadeTransition transition = new FadeTransition(Duration.millis(200), node);
        transition.setFromValue(0);
        transition.setToValue(1);
        transition.play();
    }
}
