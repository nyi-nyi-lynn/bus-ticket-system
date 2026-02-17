package com.busticket.controller;

import com.busticket.dto.BusDTO;
import com.busticket.dto.RouteDTO;
import com.busticket.dto.SalesReportDTO;
import com.busticket.dto.TripDTO;
import com.busticket.remote.BusTicketRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.util.Navigator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class AdminDashBoardController {
    @FXML
    private VBox overviewSection;
    @FXML
    private VBox busRouteSection;
    @FXML
    private VBox scheduleSection;
    @FXML
    private VBox reportSection;
    @FXML
    private Button overviewNavButton;
    @FXML
    private Button busRouteNavButton;
    @FXML
    private Button scheduleNavButton;
    @FXML
    private Button reportNavButton;

    @FXML
    private Label todayRevenueLabel;
    @FXML
    private Label todayBookingsLabel;
    @FXML
    private Label openTripsLabel;
    @FXML
    private TableView<TripDTO> overviewTripsTable;
    @FXML
    private TableColumn<TripDTO, String> overviewBusColumn;
    @FXML
    private TableColumn<TripDTO, String> overviewFromColumn;
    @FXML
    private TableColumn<TripDTO, String> overviewToColumn;
    @FXML
    private TableColumn<TripDTO, LocalDate> overviewDateColumn;
    @FXML
    private TableColumn<TripDTO, String> overviewDepartureColumn;
    @FXML
    private TableColumn<TripDTO, Double> overviewPriceColumn;
    @FXML
    private TableColumn<TripDTO, String> overviewStatusColumn;

    @FXML
    private TextField busNumberField;
    @FXML
    private TextField busTypeField;
    @FXML
    private TextField totalSeatsField;

    @FXML
    private TextField originCityField;
    @FXML
    private TextField destinationCityField;
    @FXML
    private TextField distanceField;
    @FXML
    private TextField durationField;

    @FXML
    private TextField tripBusIdField;
    @FXML
    private TextField tripRouteIdField;
    @FXML
    private DatePicker tripDatePicker;
    @FXML
    private TextField departureTimeField;
    @FXML
    private TextField arrivalTimeField;
    @FXML
    private TextField tripPriceField;

    @FXML
    private DatePicker reportFromDatePicker;
    @FXML
    private DatePicker reportToDatePicker;

    @FXML
    private Label statusLabel;

    private BusTicketRemote busTicketRemote;

    public void initialize() {
        setupOverviewTable();
        showSection(overviewSection);
        try {
            busTicketRemote = RMIClient.getBusTicketRemote();
            statusLabel.setText("Connected to server.");
            loadOverview();
        } catch (Exception e) {
            statusLabel.setText("Cannot connect to server.");
        }
    }

    @FXML
    public void handleAddBus() {
        try {
            BusDTO bus = new BusDTO();
            bus.setBusNumber(busNumberField.getText().trim());
            bus.setType(busTypeField.getText().trim());
            bus.setTotalSeats(Integer.parseInt(totalSeatsField.getText().trim()));
            boolean success = busTicketRemote.addBus(bus);
            statusLabel.setText(success ? "Bus added successfully." : "Failed to add bus.");
            if (success) {
                clearBusFields();
                loadOverview();
            }
        } catch (Exception e) {
            statusLabel.setText("Add bus error: " + e.getMessage());
        }
    }

    @FXML
    public void handleAddRoute() {
        try {
            RouteDTO route = new RouteDTO();
            route.setOriginCity(originCityField.getText().trim());
            route.setDestinationCity(destinationCityField.getText().trim());
            route.setDistanceKm(Double.parseDouble(distanceField.getText().trim()));
            route.setEstimatedDuration(durationField.getText().trim());
            boolean success = busTicketRemote.addRoute(route);
            statusLabel.setText(success ? "Route added successfully." : "Failed to add route.");
            if (success) {
                clearRouteFields();
            }
        } catch (Exception e) {
            statusLabel.setText("Add route error: " + e.getMessage());
        }
    }

    @FXML
    public void handleCreateTrip() {
        try {
            TripDTO trip = new TripDTO();
            trip.setBusId(Long.parseLong(tripBusIdField.getText().trim()));
            trip.setRouteId(Long.parseLong(tripRouteIdField.getText().trim()));
            trip.setTravelDate(tripDatePicker.getValue());
            trip.setDepartureTime(LocalTime.parse(departureTimeField.getText().trim()));
            trip.setArrivalTime(LocalTime.parse(arrivalTimeField.getText().trim()));
            trip.setPrice(Double.parseDouble(tripPriceField.getText().trim()));
            trip.setStatus("OPEN");
            boolean success = busTicketRemote.createTrip(trip);
            statusLabel.setText(success ? "Trip created successfully." : "Failed to create trip.");
            if (success) {
                clearTripFields();
                loadOverview();
            }
        } catch (Exception e) {
            statusLabel.setText("Create trip error: " + e.getMessage());
        }
    }

    @FXML
    public void handleGenerateReport() {
        try {
            SalesReportDTO report = busTicketRemote.getSalesReport(
                    reportFromDatePicker.getValue(),
                    reportToDatePicker.getValue()
            );
            if (report == null) {
                statusLabel.setText("No report data.");
                return;
            }
            statusLabel.setText("Bookings: " + report.getTotalConfirmedBookings()
                    + " | Revenue: " + report.getTotalRevenue());
            todayBookingsLabel.setText(String.valueOf(report.getTotalConfirmedBookings()));
            todayRevenueLabel.setText(String.format("$%.2f", report.getTotalRevenue()));
        } catch (Exception e) {
            statusLabel.setText("Report error: " + e.getMessage());
        }
    }

    @FXML
    public void openOverview() {
        showSection(overviewSection);
        loadOverview();
    }

    @FXML
    public void openBusRouteSection() {
        showSection(busRouteSection);
    }

    @FXML
    public void openScheduleSection() {
        showSection(scheduleSection);
    }

    @FXML
    public void openReportSection() {
        showSection(reportSection);
        if (reportFromDatePicker.getValue() == null) {
            reportFromDatePicker.setValue(LocalDate.now());
        }
        if (reportToDatePicker.getValue() == null) {
            reportToDatePicker.setValue(LocalDate.now());
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        Navigator.switchScene(getStage(event), "/com/busticket/view/login.fxml");
    }

    private void setupOverviewTable() {
        overviewBusColumn.setCellValueFactory(new PropertyValueFactory<>("busNumber"));
        overviewFromColumn.setCellValueFactory(new PropertyValueFactory<>("originCity"));
        overviewToColumn.setCellValueFactory(new PropertyValueFactory<>("destinationCity"));
        overviewDateColumn.setCellValueFactory(new PropertyValueFactory<>("travelDate"));
        overviewDepartureColumn.setCellValueFactory(new PropertyValueFactory<>("departureTime"));
        overviewPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        overviewStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadOverview() {
        if (busTicketRemote == null) {
            return;
        }
        try {
            List<TripDTO> trips = busTicketRemote.getAdvertisedTrips();
            overviewTripsTable.setItems(FXCollections.observableArrayList(trips));
            long openCount = trips.stream().filter(t -> "OPEN".equalsIgnoreCase(t.getStatus())).count();
            openTripsLabel.setText(String.valueOf(openCount));

            LocalDate today = LocalDate.now();
            SalesReportDTO todayReport = busTicketRemote.getSalesReport(today, today);
            if (todayReport != null) {
                todayBookingsLabel.setText(String.valueOf(todayReport.getTotalConfirmedBookings()));
                todayRevenueLabel.setText(String.format("$%.2f", todayReport.getTotalRevenue()));
            }
        } catch (Exception e) {
            statusLabel.setText("Overview load error: " + e.getMessage());
        }
    }

    private void showSection(VBox targetSection) {
        setVisibleSection(overviewSection, targetSection == overviewSection);
        setVisibleSection(busRouteSection, targetSection == busRouteSection);
        setVisibleSection(scheduleSection, targetSection == scheduleSection);
        setVisibleSection(reportSection, targetSection == reportSection);
        updateNavState(targetSection);
    }

    private void setVisibleSection(VBox section, boolean visible) {
        if (section == null) {
            return;
        }
        section.setVisible(visible);
        section.setManaged(visible);
    }

    private void updateNavState(VBox targetSection) {
        setNavActive(overviewNavButton, targetSection == overviewSection);
        setNavActive(busRouteNavButton, targetSection == busRouteSection);
        setNavActive(scheduleNavButton, targetSection == scheduleSection);
        setNavActive(reportNavButton, targetSection == reportSection);
    }

    private void setNavActive(Button button, boolean active) {
        if (button == null) {
            return;
        }
        if (active) {
            if (!button.getStyleClass().contains("nav-active")) {
                button.getStyleClass().add("nav-active");
            }
        } else {
            button.getStyleClass().remove("nav-active");
        }
    }

    private void clearBusFields() {
        busNumberField.clear();
        busTypeField.clear();
        totalSeatsField.clear();
    }

    private void clearRouteFields() {
        originCityField.clear();
        destinationCityField.clear();
        distanceField.clear();
        durationField.clear();
    }

    private void clearTripFields() {
        tripBusIdField.clear();
        tripRouteIdField.clear();
        tripDatePicker.setValue(null);
        departureTimeField.clear();
        arrivalTimeField.clear();
        tripPriceField.clear();
    }

    private Stage getStage(ActionEvent event) {
        return (Stage) ((Node) event.getSource()).getScene().getWindow();
    }
}
