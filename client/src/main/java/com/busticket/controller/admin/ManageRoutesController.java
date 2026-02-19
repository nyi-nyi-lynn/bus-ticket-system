package com.busticket.controller.admin;

import com.busticket.remote.RouteRemote;
import com.busticket.rmi.RMIClient;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class ManageRoutesController {
    @FXML private TableView<?> routesTable;
    @FXML private TableColumn<?, ?> colRouteId;
    @FXML private TableColumn<?, ?> colOrigin;
    @FXML private TableColumn<?, ?> colDestination;
    @FXML private TableColumn<?, ?> colDistance;
    @FXML private TableColumn<?, ?> colStatus;

    private RouteRemote routeRemote;

    @FXML
    private void initialize() {
        try {
            routeRemote = RMIClient.getRouteRemote();
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
