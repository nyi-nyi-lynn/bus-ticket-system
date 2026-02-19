package com.busticket.controller.passenger;

import com.busticket.remote.BookingRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class MyBookingsController {
    @FXML private TableView<?> bookingTable;
    @FXML private TableColumn<?, ?> colBookingId;
    @FXML private TableColumn<?, ?> colTrip;
    @FXML private TableColumn<?, ?> colSeats;
    @FXML private TableColumn<?, ?> colStatus;
    @FXML private TableColumn<?, ?> colDate;

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
    private void onViewTicket() {
        SceneSwitcher.switchContent("/com/busticket/view/passenger/TicketSuccessView.fxml");
    }
}
