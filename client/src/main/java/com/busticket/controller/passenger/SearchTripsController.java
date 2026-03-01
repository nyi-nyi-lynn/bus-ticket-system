package com.busticket.controller.passenger;

import com.busticket.dto.TripDTO;
import com.busticket.exception.UnauthorizedException;
import com.busticket.remote.TripRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SearchTripsController {
    @FXML private ComboBox<String> originCombo;
    @FXML private ComboBox<String> destinationCombo;
    @FXML private DatePicker travelDatePicker;
    @FXML private VBox resultContainer;
    @FXML private ScrollPane resultScrollPane;
    @FXML private Label resultsTitleLabel;

    private TripRemote tripRemote;
    private List<TripDTO> allTrips = new ArrayList<>();

    @FXML
    private void initialize() {
        configureResultContainer();

        LocalDate today = LocalDate.now();
        travelDatePicker.setValue(today);
        resultsTitleLabel.setText("Today's Trips");
        showEmptyState();

        loadTripsAsync();
    }

    @FXML
    private void handleSearch() {
        String origin = originCombo.getValue();
        String destination = destinationCombo.getValue();
        LocalDate date = travelDatePicker.getValue();

        if (isBlank(origin) || isBlank(destination) || date == null) {
            showSimpleAlert(Alert.AlertType.WARNING, "Missing fields", "Please complete the search", "Origin, destination, and date are required.");
            return;
        }

        try {
            List<TripDTO> filtered;
            if (tripRemote != null) {
                filtered = tripRemote.searchTrips(origin, destination, date);
            } else {
                filtered = allTrips.stream()
                        .filter(t -> origin.equals(t.getOriginCity()))
                        .filter(t -> destination.equals(t.getDestinationCity()))
                        .filter(t -> date.equals(t.getTravelDate()))
                        .collect(Collectors.toList());
            }
            resultsTitleLabel.setText("Search Results");
            displayTrips(filtered == null ? List.of() : filtered);
        } catch (Exception ex) {
            showSimpleAlert(Alert.AlertType.ERROR, "Search Failed", "Unable to search trips", ex.getMessage());
        }
    }

    private void populateCityLists() {
        Set<String> origins = new TreeSet<>();
        Set<String> destinations = new TreeSet<>();

        for (TripDTO t : allTrips) {
            if (!isBlank(t.getOriginCity())) {
                origins.add(t.getOriginCity());
            }
            if (!isBlank(t.getDestinationCity())) {
                destinations.add(t.getDestinationCity());
            }
        }

        originCombo.setItems(FXCollections.observableArrayList(origins));
        destinationCombo.setItems(FXCollections.observableArrayList(destinations));
    }

    private void loadTripsAsync() {
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        tripRemote = RMIClient.getTripRemote();
                        return tripRemote.getAllTrips();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .whenComplete((fetched, throwable) -> Platform.runLater(() -> {
                    if (throwable != null) {
                        showSimpleAlert(Alert.AlertType.ERROR, "Error", "Failed to load trips", throwable.getCause() == null ? throwable.getMessage() : throwable.getCause().getMessage());
                        return;
                    }
                    allTrips = fetched == null ? new ArrayList<>() : new ArrayList<>(fetched);
                    populateCityLists();
                    LocalDate today = LocalDate.now();
                    List<TripDTO> todays = allTrips.stream()
                            .filter(t -> t.getTravelDate() != null && t.getTravelDate().equals(today))
                            .collect(Collectors.toList());
                    displayTrips(todays);
                }));
    }

    private void displayTrips(List<TripDTO> trips) {
        resultContainer.getChildren().clear();

        if (trips == null || trips.isEmpty()) {
            showEmptyState();
            return;
        }

        resultContainer.setAlignment(Pos.TOP_LEFT);

        for (TripDTO trip : trips) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/busticket/view/components/TripCard.fxml"));
                Node card = loader.load();
                if (card instanceof Region region) {
                    region.setMaxWidth(Double.MAX_VALUE);
                    region.prefWidthProperty().bind(resultContainer.widthProperty().subtract(24));
                }
                com.busticket.controller.components.TripCardController controller = loader.getController();
                controller.setData(trip);
                controller.setOnBookNow(this::navigateToBooking);
                resultContainer.getChildren().add(card);
            } catch (Exception ex) {
                showSimpleAlert(Alert.AlertType.ERROR, "Error", "Failed to render trip", ex.getMessage());
            }
        }
    }

    private void showEmptyState() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/busticket/view/components/EmptyState.fxml"));
            Node empty = loader.load();
            empty.setOpacity(0);
            resultContainer.setAlignment(Pos.CENTER);
            resultContainer.getChildren().setAll(empty);
            FadeTransition ft = new FadeTransition(Duration.millis(180), empty);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        } catch (Exception ex) {
            showSimpleAlert(Alert.AlertType.ERROR, "Error", "Failed to render empty state", ex.getMessage());
        }
    }

    private void configureResultContainer() {
        resultContainer.setPadding(new Insets(20));
        resultContainer.setAlignment(Pos.TOP_LEFT);

        if (resultScrollPane != null) {
            // allow natural content height growth so vertical scrolling works.
            resultScrollPane.setFitToHeight(false);
            resultScrollPane.setPannable(true);
            resultScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
                double contentHeight = resultContainer.getBoundsInLocal().getHeight();
                if (contentHeight <= 0) {
                    return;
                }
                double delta = event.getDeltaY();
                double newVvalue = resultScrollPane.getVvalue() - delta / contentHeight;
                resultScrollPane.setVvalue(Math.max(0, Math.min(1, newVvalue)));
                event.consume();
            });
        }
    }

    private void navigateToBooking(TripDTO trip) {
        try {
            selectSeat(trip);
        } catch (UnauthorizedException ex) {
            showLoginRequiredAndRedirect(ex.getMessage());
        }
    }

    private void selectSeat(TripDTO trip) throws UnauthorizedException {
        if (trip == null) {
            return;
        }
        if (Session.getCurrentUser() == null || Session.getCurrentUser().getUserId() == null) {
            throw new UnauthorizedException("Please login to continue booking");
        }
        SceneSwitcher.switchToSeatSelection(trip);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void showSimpleAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showLoginRequiredAndRedirect(String message) {
        showSimpleAlert(
                Alert.AlertType.WARNING,
                "Login Required",
                "Please login to continue booking",
                message == null || message.isBlank() ? "Please login to continue booking" : message
        );
        Session.clearPendingSelection();
        Session.clearBookingContext();
        SceneSwitcher.resetToAuth("/com/busticket/view/auth/LoginView.fxml");
    }
}
