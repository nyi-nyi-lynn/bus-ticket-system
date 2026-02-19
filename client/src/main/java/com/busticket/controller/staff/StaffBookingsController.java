package com.busticket.controller.staff;

import com.busticket.remote.BookingRemote;
import com.busticket.rmi.RMIClient;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class StaffBookingsController {
    @FXML private TableView<?> bookingsTable;
    @FXML private TableColumn<?, ?> colBookingId;
    @FXML private TableColumn<?, ?> colPassenger;
    @FXML private TableColumn<?, ?> colSeats;
    @FXML private TableColumn<?, ?> colStatus;

    private BookingRemote bookingRemote;

    @FXML
    private void initialize() {
        try {
            bookingRemote = RMIClient.getBookingRemote();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    private void onUpdate() {
    }

    @FXML
    private void onCancel() {
    }
}
