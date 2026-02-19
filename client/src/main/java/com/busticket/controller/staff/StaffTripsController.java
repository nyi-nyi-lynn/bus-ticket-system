package com.busticket.controller.staff;

import com.busticket.remote.TripRemote;
import com.busticket.rmi.RMIClient;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class StaffTripsController {
    @FXML private TableView<?> tripsTable;
    @FXML private TableColumn<?, ?> colTripId;
    @FXML private TableColumn<?, ?> colRoute;
    @FXML private TableColumn<?, ?> colDeparture;
    @FXML private TableColumn<?, ?> colStatus;

    private TripRemote tripRemote;

    @FXML
    private void initialize() {
        try {
            tripRemote = RMIClient.getTripRemote();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    private void onRefresh() {
    }
}
