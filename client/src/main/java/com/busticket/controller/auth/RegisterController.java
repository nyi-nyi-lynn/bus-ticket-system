package com.busticket.controller.auth;

import com.busticket.dto.UserDTO;
import com.busticket.remote.UserRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    private UserRemote userRemote;

    @FXML
    private void initialize() {
        try {
            userRemote = RMIClient.getUserRemote();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    private void onRegister() {
        try {
            UserDTO user = new UserDTO();
            user.setName(nameField.getText());
            user.setEmail(emailField.getText());
            user.setPhone(phoneField.getText());
            user.setPassword(passwordField.getText());
            userRemote.register(user);
            SceneSwitcher.showAuth("/com/busticket/view/auth/LoginView.fxml");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    private void onBack() {
        SceneSwitcher.showAuth("/com/busticket/view/auth/LoginView.fxml");
    }
}
