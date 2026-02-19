package com.busticket.controller.admin;

import com.busticket.remote.BusRemote;
import com.busticket.rmi.RMIClient;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class ManageBusesController {
    @FXML private TableView<?> busTable;
    @FXML private TableColumn<?, ?> colBusId;
    @FXML private TableColumn<?, ?> colPlate;
    @FXML private TableColumn<?, ?> colType;
    @FXML private TableColumn<?, ?> colCapacity;
    @FXML private TableColumn<?, ?> colStatus;

    private BusRemote busRemote;

    @FXML
    private void initialize() {
        try {
            busRemote = RMIClient.getBusRemote();
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
