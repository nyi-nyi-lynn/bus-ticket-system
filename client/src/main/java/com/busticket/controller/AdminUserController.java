package com.busticket.controller;

import com.busticket.dto.UserDTO;
import com.busticket.remote.UserRemote;
import com.busticket.rmi.RMIClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;

import java.util.List;

public class AdminUserController {
    @FXML private TableView<UserDTO> userTable;

    private UserRemote userRemote;

    @FXML
    public void initialize() {

        try {
            userRemote = RMIClient.getUserRemote();
            List<UserDTO> users = userRemote.getAllUsers();
            ObservableList<UserDTO> list = FXCollections.observableArrayList(users);
            userTable.setItems(list);
        } catch (Exception e) {
            System.err.println("RMI Connection Error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Server Error", "Cannot connect to RMI Server");
        }
    }


    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
