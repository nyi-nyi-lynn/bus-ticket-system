package com.busticket.controller;

import com.busticket.dto.UserDTO;
import com.busticket.remote.UserRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.util.Navigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.stage.Stage;

public class RegisterController {


    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;

    private UserRemote userRemote;


    @FXML
    public void initialize() {
        try {
            userRemote = RMIClient.getUserRemote();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Connection Error", "Cannot connect to RMI Server");
        }
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        try {
            //validation
            if (nameField.getText().isEmpty() || emailField.getText().isEmpty() || passwordField.getText().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill required fields");
                return;
            }

            UserDTO dto = new UserDTO();
            dto.setName(nameField.getText());
            dto.setEmail(emailField.getText());
            dto.setPhone(phoneField.getText());
            dto.setPassword(passwordField.getText());
            dto.setRole("PASSENGER");
            dto.setStatus("ACTIVE");

            boolean success = userRemote.register(dto);

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Register Success", "Registration Successful");
                Navigator.switchScene(getStage(event), "/com/busticket/view/login.fxml");
            } else {
                showAlert(Alert.AlertType.ERROR, "Register Failed", "Email might already exist!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    @FXML
    public void goToLogin(ActionEvent event) {
        Navigator.switchScene(getStage(event), "/com/busticket/view/login.fxml");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Stage getStage(ActionEvent event) {
        return (Stage) ((Node) event.getSource()).getScene().getWindow();
    }
}
