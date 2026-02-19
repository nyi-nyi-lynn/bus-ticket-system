package com.busticket.controller.passenger;

import com.busticket.dto.UserDTO;
import com.busticket.remote.UserRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class ProfileController {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    private UserRemote userRemote;

    @FXML
    private void initialize() {
        try {
            userRemote = RMIClient.getUserRemote();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        UserDTO user = Session.getCurrentUser();
        if (user != null) {
            nameField.setText(user.getName());
            emailField.setText(user.getEmail());
            phoneField.setText(user.getPhone());
        }
    }

    @FXML
    private void onSave() {
        try {
            UserDTO user = Session.getCurrentUser();
            if (user != null) {
                user.setName(nameField.getText());
                user.setEmail(emailField.getText());
                user.setPhone(phoneField.getText());
                userRemote.updateUser(user);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
