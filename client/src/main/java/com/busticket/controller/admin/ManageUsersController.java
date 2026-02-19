package com.busticket.controller.admin;

import com.busticket.remote.UserRemote;
import com.busticket.rmi.RMIClient;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class ManageUsersController {
    @FXML private TableView<?> usersTable;
    @FXML private TableColumn<?, ?> colUserId;
    @FXML private TableColumn<?, ?> colName;
    @FXML private TableColumn<?, ?> colEmail;
    @FXML private TableColumn<?, ?> colRole;
    @FXML private TableColumn<?, ?> colStatus;

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
    private void onAdd() {
    }

    @FXML
    private void onUpdate() {
    }

    @FXML
    private void onBlock() {
    }
}
