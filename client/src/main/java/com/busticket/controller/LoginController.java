package com.busticket.controller;

import com.busticket.dto.UserDTO;
import com.busticket.remote.UserRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.util.Navigator;
import com.busticket.util.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    private UserRemote userRemote;

    public void initialize() {
        try {
            userRemote = RMIClient.getUserRemote();
        } catch (Exception e) {
            System.err.println("RMI Connection Error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Server Error", "Cannot connect to RMI Server");
        }
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        try {
            String email = emailField.getText();
            String password = passwordField.getText();

            if (email.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill all fields");
                return;
            }

            UserDTO user = userRemote.login(email, password);

            if (user == null) {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid email or password");
                return;
            }


            Session.setCurrentUser(user);

            if ("ADMIN".equals(user.getRole())) {
                Navigator.switchScene(getStage(event), "/com/busticket/view/admin/admin_dashboard.fxml");
            } else if ("STAFF".equals(user.getRole())) {
                Navigator.switchScene(getStage(event), "/com/busticket/view/staff_dashboard.fxml");
            } else {
                Navigator.switchScene(getStage(event), "/com/busticket/view/passenger_dashboard.fxml");
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "System Error", e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void goToRegister(ActionEvent event) {
        Navigator.switchScene(getStage(event), "/com/busticket/view/auth/register.fxml");
    }

    private Stage getStage(ActionEvent event) {
        return (Stage) ((Node) event.getSource()).getScene().getWindow();
    }
}
