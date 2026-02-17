package com.busticket.controller;

import com.busticket.util.Navigator;
import com.busticket.util.PassengerViewRouter;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Map;

public class PassengerDashBoardController {
    @FXML
    private StackPane passengerContentPane;
    @FXML
    private Button routesNavButton;
    @FXML
    private Button orderNavButton;
    @FXML
    private Button paymentNavButton;
    @FXML
    private Button ticketNavButton;

    private final Map<String, String> pageMap = Map.of(
            "routes", "/com/busticket/view/passenger_route_search.fxml",
            "order", "/com/busticket/view/passenger_order.fxml",
            "payment", "/com/busticket/view/passenger_payment.fxml",
            "ticket", "/com/busticket/view/passenger_ticket.fxml"
    );

    public void initialize() {
        PassengerViewRouter.setPageOpener(this::openPage);
        openPage("routes");
    }

    @FXML
    public void openRoutesPage() {
        openPage("routes");
    }

    @FXML
    public void openOrderPage() {
        openPage("order");
    }

    @FXML
    public void openPaymentPage() {
        openPage("payment");
    }

    @FXML
    public void openTicketPage() {
        openPage("ticket");
    }

    @FXML
    public void handleLogout() {
        Stage stage = (Stage) passengerContentPane.getScene().getWindow();
        Navigator.switchScene(stage, "/com/busticket/view/login.fxml");
    }

    private void openPage(String key) {
        String path = pageMap.get(key);
        if (path == null) {
            return;
        }
        try {
            Parent view = FXMLLoader.load(getClass().getResource(path));
            passengerContentPane.getChildren().setAll(view);
            updateNavState(key);
            FadeTransition fade = new FadeTransition(Duration.millis(160), view);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        } catch (IOException e) {
            throw new RuntimeException("Cannot load passenger page: " + path, e);
        }
    }

    private void updateNavState(String pageKey) {
        setNavActive(routesNavButton, "routes".equals(pageKey));
        setNavActive(orderNavButton, "order".equals(pageKey));
        setNavActive(paymentNavButton, "payment".equals(pageKey));
        setNavActive(ticketNavButton, "ticket".equals(pageKey));
    }

    private void setNavActive(Button button, boolean active) {
        if (button == null) {
            return;
        }
        if (active) {
            if (!button.getStyleClass().contains("nav-active")) {
                button.getStyleClass().add("nav-active");
            }
        } else {
            button.getStyleClass().remove("nav-active");
        }
    }
}
