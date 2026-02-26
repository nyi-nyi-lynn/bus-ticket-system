package com.busticket.controller.passenger;

import com.busticket.dto.BookingSummaryDTO;
import com.busticket.dto.PassengerDashboardDTO;
import com.busticket.dto.RecentBookingDTO;
import com.busticket.dto.UpcomingTripDTO;
import com.busticket.dto.UserSummaryDTO;
import com.busticket.enums.Role;
import com.busticket.exception.UnauthorizedException;
import com.busticket.exception.ValidationException;
import com.busticket.remote.PassengerRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PassengerDashboardController {
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Label welcomeLabel;
    @FXML private Button viewTicketButton;
    @FXML private VBox quickActionsSection;
    @FXML private FlowPane quickActionsWrap;
    @FXML private VBox searchTripsCard;
    @FXML private VBox myBookingsCard;
    @FXML private VBox profileCard;

    @FXML private Label upcomingRouteLabel;
    @FXML private Label upcomingDepartureLabel;
    @FXML private Label upcomingBusLabel;
    @FXML private Label upcomingSeatsLabel;
    @FXML private Label upcomingCodeLabel;

    @FXML private Label totalBookingsValue;
    @FXML private Label upcomingTripsValue;
    @FXML private Label completedTripsValue;
    @FXML private Label cancelledBookingsValue;

    @FXML private TableView<RecentBookingDTO> recentBookingsTable;
    @FXML private TableColumn<RecentBookingDTO, String> bookingCodeColumn;
    @FXML private TableColumn<RecentBookingDTO, String> routeColumn;
    @FXML private TableColumn<RecentBookingDTO, String> departureColumn;
    @FXML private TableColumn<RecentBookingDTO, String> seatsColumn;
    @FXML private TableColumn<RecentBookingDTO, String> totalPriceColumn;
    @FXML private TableColumn<RecentBookingDTO, String> statusColumn;
    @FXML private TableColumn<RecentBookingDTO, String> actionColumn;

    @FXML private VBox notificationsBox;
    @FXML private StackPane loadingOverlay;

    private final ObservableList<RecentBookingDTO> recentBookingsData = FXCollections.observableArrayList();
    private PassengerRemote passengerRemote;
    private Long currentUserId;
    private UpcomingTripDTO currentUpcomingTrip;
    private boolean loading;

    @FXML
    private void initialize() {
        try {
            passengerRemote = RMIClient.getPassengerRemote();
        } catch (Exception ex) {
            passengerRemote = null;
            Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, "Service Unavailable",
                            "Passenger dashboard service is not available.",
                            ex.getMessage()));
        }

        currentUserId = Session.getCurrentUser() == null ? null : Session.getCurrentUser().getUserId();
        setupQuickActions();
        setupTable();
        if (passengerRemote != null) {
            loadDashboard();
        }
    }

    @FXML
    private void onSearchTrips() {
        SceneSwitcher.switchContent("/com/busticket/view/passenger/SearchTripsView.fxml");
    }

    @FXML
    private void onMyBookings() {
        SceneSwitcher.switchContent("/com/busticket/view/passenger/MyBookingsView.fxml");
    }

    @FXML
    private void onProfile() {
        SceneSwitcher.switchContent("/com/busticket/view/passenger/PassengerProfile.fxml");
    }

    @FXML
    private void onViewTicket() {
        if (currentUpcomingTrip == null || currentUpcomingTrip.getBookingId() == null) {
            showAlert(Alert.AlertType.INFORMATION, "No Upcoming Trip",
                    "There is no upcoming trip to view.", "Please check your bookings.");
            return;
        }
        Session.setCurrentBookingContext(currentUpcomingTrip.getBookingId(),
                currentUpcomingTrip.getBookingCode(), null);
        SceneSwitcher.switchContent("/com/busticket/view/passenger/TicketView.fxml");
    }

    private void setupTable() {
        recentBookingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        recentBookingsTable.setItems(recentBookingsData);
        recentBookingsTable.setPlaceholder(new Label("No recent bookings."));

        bookingCodeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safe(data.getValue().getBookingCode())));
        routeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(routeLabel(data.getValue())));
        departureColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatDateTime(
                data.getValue().getTravelDate(), data.getValue().getDepartureTime())));
        seatsColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(seatsLabel(data.getValue())));
        totalPriceColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatMoney(data.getValue().getTotalPrice())));
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeUpper(data.getValue().getStatus())));
        actionColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(""));
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("View");

            {
                viewButton.getStyleClass().add("action-button");
                viewButton.setOnAction(event -> {
                    RecentBookingDTO booking = getTableView().getItems().get(getIndex());
                    onViewBooking(booking);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                setGraphic(viewButton);
            }
        });
    }

    private void loadDashboard() {
        if (passengerRemote == null) {
            showAlert(Alert.AlertType.ERROR, "Unavailable", "Passenger dashboard is unavailable.",
                    "Please reconnect and try again.");
            return;
        }
        if (Session.isGuest() || currentUserId == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "You must be logged in as PASSENGER.",
                    "Please sign in again.");
            return;
        }

        setLoading(true);
        Task<PassengerDashboardDTO> task = new Task<>() {
            @Override
            protected PassengerDashboardDTO call() throws Exception {
                return passengerRemote.getDashboard(currentUserId);
            }
        };

        task.setOnSucceeded(event -> Platform.runLater(() -> {
            applyDashboard(task.getValue());
            setLoading(false);
        }));

        task.setOnFailed(event -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            if (ex instanceof ValidationException validation) {
                showAlert(Alert.AlertType.WARNING, "Unauthorized",
                        "You are not authorized to view this dashboard.", validation.getMessage());
            } else if (ex instanceof UnauthorizedException unauthorized) {
                showAlert(Alert.AlertType.WARNING, "Unauthorized",
                        "You are not authorized to view this dashboard.", unauthorized.getMessage());
            } else {
                showAlert(Alert.AlertType.ERROR, "Load Failed", "Unable to load dashboard.",
                        ex == null ? "Unexpected error." : ex.getMessage());
            }
            setLoading(false);
        }));

        startBackgroundTask(task, "passenger-dashboard-load-task");
    }

    private void applyDashboard(PassengerDashboardDTO response) {
        UserSummaryDTO userSummary = response == null ? null : response.getUserSummary();
        BookingSummaryDTO bookingSummary = response == null ? null : response.getBookingSummary();
        UpcomingTripDTO upcomingTrip = response == null ? null : response.getUpcomingTrip();
        List<RecentBookingDTO> recentBookings = response == null ? List.of() : response.getRecentBookings();
        List<String> notifications = response == null ? List.of() : response.getNotifications();

        String name = userSummary == null ? "-" : safe(userSummary.getName());
        welcomeLabel.setText("Welcome, " + name);

        updateSummary(bookingSummary);
        updateUpcoming(upcomingTrip);
        recentBookingsData.setAll(recentBookings == null ? List.of() : recentBookings);
        updateNotifications(notifications);
    }

    private void updateSummary(BookingSummaryDTO summary) {
        if (summary == null) {
            totalBookingsValue.setText("-");
            upcomingTripsValue.setText("-");
            completedTripsValue.setText("-");
            cancelledBookingsValue.setText("-");
            return;
        }
        totalBookingsValue.setText(String.valueOf(summary.getTotalBookings()));
        upcomingTripsValue.setText(String.valueOf(summary.getUpcomingTrips()));
        completedTripsValue.setText(String.valueOf(summary.getCompletedTrips()));
        cancelledBookingsValue.setText(String.valueOf(summary.getCancelledBookings()));
    }

    private void updateUpcoming(UpcomingTripDTO trip) {
        currentUpcomingTrip = trip;
        if (trip == null) {
            upcomingRouteLabel.setText("-");
            upcomingDepartureLabel.setText("-");
            upcomingBusLabel.setText("-");
            upcomingSeatsLabel.setText("-");
            upcomingCodeLabel.setText("-");
            viewTicketButton.setDisable(true);
            return;
        }
        upcomingRouteLabel.setText(routeLabel(trip));
        upcomingDepartureLabel.setText(formatDateTime(trip.getTravelDate(), trip.getDepartureTime()));
        upcomingBusLabel.setText(safe(trip.getBusNumber()));
        upcomingSeatsLabel.setText(seatsLabel(trip.getSeatNumbers()));
        upcomingCodeLabel.setText(safe(trip.getBookingCode()));
        viewTicketButton.setDisable(false);
    }

    private void updateNotifications(List<String> notifications) {
        notificationsBox.getChildren().clear();
        if (notifications == null || notifications.isEmpty()) {
            Label empty = new Label("No notifications.");
            empty.getStyleClass().add("alert-item");
            notificationsBox.getChildren().add(empty);
            return;
        }
        for (String note : notifications) {
            Label label = new Label(safe(note));
            label.getStyleClass().add("alert-item");
            notificationsBox.getChildren().add(label);
        }
    }

    private void onViewBooking(RecentBookingDTO booking) {
        if (Session.isGuest() || Session.getCurrentUser() == null || Session.getCurrentUser().getUserId() == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "You are not logged in.",
                    "Please login to view tickets.");
            return;
        }
        if (booking == null || booking.getBookingId() == null) {
            showAlert(Alert.AlertType.WARNING, "Invalid Selection", "Booking is missing.",
                    "Please refresh and try again.");
            return;
        }
        Session.setCurrentBookingContext(booking.getBookingId(), booking.getBookingCode(), booking.getTotalPrice());
        SceneSwitcher.switchContent("/com/busticket/view/passenger/TicketView.fxml");
    }

    private void setLoading(boolean value) {
        loading = value;
        setNodeDisabled(searchTripsCard, value);
        setNodeDisabled(myBookingsCard, value);
        setNodeDisabled(profileCard, value);
        setNodeDisabled(viewTicketButton, value);
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(value);
            loadingOverlay.setManaged(value);
        }
    }

    private void setupQuickActions() {
        boolean passengerOnly = !Session.isGuest() && Session.getRole() == Role.PASSENGER;
        if (quickActionsSection != null) {
            quickActionsSection.setVisible(passengerOnly);
            quickActionsSection.setManaged(passengerOnly);
        }
        if (!passengerOnly) {
            return;
        }

        if (quickActionsWrap != null && quickActionsSection != null) {
            quickActionsWrap.prefWidthProperty().bind(quickActionsSection.widthProperty());
        }

        setupCardHoverAnimation(searchTripsCard);
        setupCardHoverAnimation(myBookingsCard);
        setupCardHoverAnimation(profileCard);
    }

    private void setupCardHoverAnimation(Node card) {
        if (card == null) {
            return;
        }
        card.setOnMouseEntered(event -> animateScale(card, 1.05));
        card.setOnMouseExited(event -> animateScale(card, 1.0));
    }

    private void animateScale(Node node, double scale) {
        if (node == null) {
            return;
        }
        ScaleTransition transition = new ScaleTransition(Duration.millis(140), node);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.play();
    }

    private void setNodeDisabled(Node node, boolean disabled) {
        if (node != null) {
            node.setDisable(disabled);
        }
    }

    private String routeLabel(RecentBookingDTO booking) {
        if (booking == null) {
            return "-";
        }
        return safe(booking.getOriginCity()) + " -> " + safe(booking.getDestinationCity());
    }

    private String routeLabel(UpcomingTripDTO trip) {
        if (trip == null) {
            return "-";
        }
        return safe(trip.getOriginCity()) + " -> " + safe(trip.getDestinationCity());
    }

    private String seatsLabel(RecentBookingDTO booking) {
        if (booking == null || booking.getSeatNumbers() == null || booking.getSeatNumbers().isEmpty()) {
            return "-";
        }
        return String.join(", ", booking.getSeatNumbers());
    }

    private String seatsLabel(List<String> seats) {
        if (seats == null || seats.isEmpty()) {
            return "-";
        }
        return String.join(", ", seats);
    }

    private String formatMoney(Double value) {
        if (value == null) {
            return "-";
        }
        return MONEY_FORMAT.format(value);
    }

    private String formatDateTime(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            return "-";
        }
        return LocalDateTime.of(date, time).format(DATE_TIME_FMT);
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }

    private String safeUpper(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void startBackgroundTask(Task<?> task, String threadName) {
        Thread worker = new Thread(task, threadName);
        worker.setDaemon(true);
        worker.start();
    }
}
