package com.busticket.controller.admin;

import com.busticket.remote.BookingRemote;
import com.busticket.rmi.RMIClient;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class ReportsController {
    @FXML private TableView<?> reportsTable;
    @FXML private TableColumn<?, ?> colReportId;
    @FXML private TableColumn<?, ?> colPeriod;
    @FXML private TableColumn<?, ?> colRevenue;
    @FXML private TableColumn<?, ?> colBookings;

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
    private void onRefresh() {
    }
}
