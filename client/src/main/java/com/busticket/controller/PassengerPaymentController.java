package com.busticket.controller;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.PaymentDTO;
import com.busticket.remote.BusTicketRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.util.PassengerFlowContext;
import com.busticket.util.PassengerViewRouter;
import com.busticket.util.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.util.List;
import java.util.stream.Collectors;

public class PassengerPaymentController {
    @FXML
    private Label bookingSummaryLabel;
    @FXML
    private Label totalAmountLabel;
    @FXML
    private ComboBox<String> paymentMethodField;
    @FXML
    private Label paymentStatusLabel;
    @FXML
    private ComboBox<BookingDTO> unpaidBookingCombo;
    @FXML
    private TableView<PaymentDTO> paymentHistoryTable;
    @FXML
    private TableColumn<PaymentDTO, Long> paymentIdColumn;
    @FXML
    private TableColumn<PaymentDTO, String> paymentMethodColumn;
    @FXML
    private TableColumn<PaymentDTO, Double> paymentAmountColumn;
    @FXML
    private TableColumn<PaymentDTO, String> paymentStatusColumn;

    private BusTicketRemote busTicketRemote;
    private BookingDTO booking;

    public void initialize() {
        setupHistoryTable();
        booking = PassengerFlowContext.getCurrentBooking();
        paymentMethodField.setItems(FXCollections.observableArrayList("MOBILE_BANKING", "CARD"));
        paymentMethodField.getSelectionModel().selectFirst();
        unpaidBookingCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(BookingDTO object) {
                if (object == null) {
                    return "";
                }
                return "Order #" + object.getBookingId() + " | $" + object.getTotalPrice() + " | " + object.getStatus();
            }

            @Override
            public BookingDTO fromString(String string) {
                return null;
            }
        });

        try {
            busTicketRemote = RMIClient.getBusTicketRemote();
            loadUnpaidBookings();
            loadPaymentHistory();
        } catch (Exception e) {
            paymentStatusLabel.setText("Cannot connect to server.");
        }

        if (booking == null) {
            paymentStatusLabel.setText("Select an unpaid order to continue.");
            bookingSummaryLabel.setText("-");
            totalAmountLabel.setText("$0");
        } else {
            bookingSummaryLabel.setText("Order #" + booking.getBookingId() + " | Seats: " + String.join(", ", booking.getSeatNumbers()));
            totalAmountLabel.setText("$" + booking.getTotalPrice());
        }
    }

    @FXML
    public void handleConfirmPayment() {
        try {
            if (booking == null) {
                paymentStatusLabel.setText("No order found.");
                return;
            }
            if (busTicketRemote == null) {
                paymentStatusLabel.setText("Server not connected.");
                return;
            }
            PaymentDTO payment = busTicketRemote.makePayment(
                    booking.getBookingId(),
                    paymentMethodField.getValue(),
                    booking.getTotalPrice()
            );
            if (payment == null) {
                paymentStatusLabel.setText("Payment failed.");
                return;
            }
            paymentStatusLabel.setText("Payment " + payment.getPaymentStatus() + " for $" + payment.getPaidAmount());
            loadUnpaidBookings();
            loadPaymentHistory();
            PassengerViewRouter.open("ticket");
        } catch (Exception e) {
            paymentStatusLabel.setText("Payment error: " + e.getMessage());
        }
    }

    @FXML
    public void handleBackToOrder() {
        PassengerViewRouter.open("order");
    }

    @FXML
    public void handleSelectUnpaidBooking() {
        BookingDTO selected = unpaidBookingCombo.getValue();
        if (selected == null) {
            return;
        }
        booking = selected;
        PassengerFlowContext.setCurrentBooking(selected);
        bookingSummaryLabel.setText("Order #" + booking.getBookingId() + " | Seats: " + String.join(", ", booking.getSeatNumbers()));
        totalAmountLabel.setText("$" + booking.getTotalPrice());
    }

    private void setupHistoryTable() {
        paymentIdColumn.setCellValueFactory(new PropertyValueFactory<>("paymentId"));
        paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        paymentAmountColumn.setCellValueFactory(new PropertyValueFactory<>("paidAmount"));
        paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
    }

    private void loadUnpaidBookings() {
        try {
            if (busTicketRemote == null || Session.getUser() == null) {
                return;
            }
            List<BookingDTO> all = busTicketRemote.getBookingsByUser(Session.getUser().getUserId());
            List<BookingDTO> unpaid = all.stream()
                    .filter(b -> "PENDING".equalsIgnoreCase(b.getStatus()))
                    .collect(Collectors.toList());
            unpaidBookingCombo.setItems(FXCollections.observableArrayList(unpaid));
            if (!unpaid.isEmpty()) {
                unpaidBookingCombo.getSelectionModel().selectFirst();
                handleSelectUnpaidBooking();
            } else {
                booking = null;
                PassengerFlowContext.setCurrentBooking(null);
                bookingSummaryLabel.setText("-");
                totalAmountLabel.setText("$0");
            }
        } catch (Exception e) {
            paymentStatusLabel.setText("Cannot load unpaid bookings.");
        }
    }

    private void loadPaymentHistory() {
        try {
            if (busTicketRemote == null || Session.getUser() == null) {
                return;
            }
            List<PaymentDTO> history = busTicketRemote.getPaymentsByUser(Session.getUser().getUserId());
            paymentHistoryTable.setItems(FXCollections.observableArrayList(history));
        } catch (Exception e) {
            paymentStatusLabel.setText("Cannot load payment history.");
        }
    }
}
