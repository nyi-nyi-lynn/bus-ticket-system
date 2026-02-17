package com.busticket.controller;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.TicketDTO;
import com.busticket.remote.BusTicketRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.util.PassengerFlowContext;
import com.busticket.util.PassengerViewRouter;
import com.busticket.util.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class PassengerTicketController {
    @FXML
    private Label passengerLabel;
    @FXML
    private Label routeLabel;
    @FXML
    private Label dateTimeLabel;
    @FXML
    private Label seatsLabel;
    @FXML
    private Label ticketCodeLabel;
    @FXML
    private Label qrPayloadLabel;
    @FXML
    private Label ticketStatusLabel;
    @FXML
    private TableView<TicketDTO> ticketHistoryTable;
    @FXML
    private TableColumn<TicketDTO, String> historyTicketCodeColumn;
    @FXML
    private TableColumn<TicketDTO, String> historyRouteColumn;
    @FXML
    private TableColumn<TicketDTO, String> historyDateColumn;
    @FXML
    private TableColumn<TicketDTO, String> historyStatusColumn;

    public void initialize() {
        setupHistoryTable();
        loadTicketHistory();
        BookingDTO booking = PassengerFlowContext.getCurrentBooking();
        if (booking == null) {
            ticketStatusLabel.setText("No ticket data available.");
            return;
        }

        try {
            BusTicketRemote busTicketRemote = RMIClient.getBusTicketRemote();
            TicketDTO ticket = busTicketRemote.viewTicket(booking.getTicketCode());
            if (ticket == null) {
                ticketStatusLabel.setText("Ticket not found.");
                return;
            }
            passengerLabel.setText(ticket.getPassengerName());
            routeLabel.setText(ticket.getOriginCity() + " -> " + ticket.getDestinationCity());
            dateTimeLabel.setText(ticket.getTravelDate() + " " + ticket.getDepartureTime());
            seatsLabel.setText(String.join(", ", ticket.getSeatNumbers()));
            ticketCodeLabel.setText(ticket.getTicketCode());
            qrPayloadLabel.setText(ticket.getQrPayload());
            ticketStatusLabel.setText("Ticket is ready.");
        } catch (Exception e) {
            ticketStatusLabel.setText("Ticket load error: " + e.getMessage());
        }
    }

    @FXML
    public void handleDownloadPdf() {
        ticketStatusLabel.setText("PDF download will be added in next step.");
    }

    @FXML
    public void handleBackToRoutes() {
        PassengerViewRouter.open("routes");
    }

    @FXML
    public void handleOpenSelectedTicket() {
        TicketDTO selected = ticketHistoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        passengerLabel.setText(selected.getPassengerName());
        routeLabel.setText(selected.getOriginCity() + " -> " + selected.getDestinationCity());
        dateTimeLabel.setText(selected.getTravelDate() + " " + selected.getDepartureTime());
        seatsLabel.setText(String.join(", ", selected.getSeatNumbers()));
        ticketCodeLabel.setText(selected.getTicketCode());
        qrPayloadLabel.setText(selected.getQrPayload());
        ticketStatusLabel.setText("Loaded ticket " + selected.getTicketCode());
    }

    private void setupHistoryTable() {
        historyTicketCodeColumn.setCellValueFactory(new PropertyValueFactory<>("ticketCode"));
        historyRouteColumn.setCellValueFactory(new PropertyValueFactory<>("originCity"));
        historyDateColumn.setCellValueFactory(new PropertyValueFactory<>("travelDate"));
        historyStatusColumn.setCellValueFactory(new PropertyValueFactory<>("bookingStatus"));
    }

    private void loadTicketHistory() {
        try {
            if (Session.getUser() == null) {
                return;
            }
            BusTicketRemote busTicketRemote = RMIClient.getBusTicketRemote();
            List<TicketDTO> history = busTicketRemote.getTicketsByUser(Session.getUser().getUserId());
            ticketHistoryTable.setItems(FXCollections.observableArrayList(history));
        } catch (Exception e) {
            ticketStatusLabel.setText("Cannot load ticket history.");
        }
    }
}
