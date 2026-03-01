package com.busticket.controller.admin;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.ChartPointDTO;
import com.busticket.dto.DashboardResponseDTO;
import com.busticket.dto.KpiDTO;
import com.busticket.dto.RoutePopularityRowDTO;
import com.busticket.exception.ValidationException;
import com.busticket.remote.DashboardRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminDashboardController {
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML private Button refreshButton;
    @FXML private Label generatedAtLabel;
    @FXML private StackPane loadingOverlay;

    @FXML private Label kpiTitle1;
    @FXML private Label kpiValue1;
    @FXML private Label kpiSubtitle1;
    @FXML private Label kpiTitle2;
    @FXML private Label kpiValue2;
    @FXML private Label kpiSubtitle2;
    @FXML private Label kpiTitle3;
    @FXML private Label kpiValue3;
    @FXML private Label kpiSubtitle3;
    @FXML private Label kpiTitle4;
    @FXML private Label kpiValue4;
    @FXML private Label kpiSubtitle4;
    @FXML private Label kpiTitle5;
    @FXML private Label kpiValue5;
    @FXML private Label kpiSubtitle5;
    @FXML private Label kpiTitle6;
    @FXML private Label kpiValue6;
    @FXML private Label kpiSubtitle6;

    @FXML private LineChart<String, Number> revenueChart;
    @FXML private CategoryAxis revenueXAxis;
    @FXML private NumberAxis revenueYAxis;
    @FXML private PieChart bookingStatusChart;

    @FXML private TableView<RoutePopularityRowDTO> topRoutesTable;
    @FXML private TableColumn<RoutePopularityRowDTO, String> topRouteNoColumn;
    @FXML private TableColumn<RoutePopularityRowDTO, String> topRouteLabelColumn;
    @FXML private TableColumn<RoutePopularityRowDTO, String> topRouteBookingsColumn;
    @FXML private TableColumn<RoutePopularityRowDTO, String> topRouteRevenueColumn;

    @FXML private TableView<BookingDTO> recentBookingsTable;
    @FXML private TableColumn<BookingDTO, String> bookingIdColumn;
    @FXML private TableColumn<BookingDTO, String> bookingPassengerColumn;
    @FXML private TableColumn<BookingDTO, String> bookingRouteColumn;
    @FXML private TableColumn<BookingDTO, String> bookingTravelDateColumn;
    @FXML private TableColumn<BookingDTO, String> bookingStatusColumn;
    @FXML private TableColumn<BookingDTO, String> bookingTotalColumn;
    @FXML private TableColumn<BookingDTO, String> bookingBookedAtColumn;

    @FXML private VBox alertsBox;

    private final ObservableList<RoutePopularityRowDTO> topRoutesData = FXCollections.observableArrayList();
    private final ObservableList<BookingDTO> recentBookingsData = FXCollections.observableArrayList();
    private DashboardRemote dashboardRemote;
    private Long currentAdminUserId;
    private boolean loading;

    @FXML
    private void initialize() {
        try {
            dashboardRemote = RMIClient.getDashboardRemote();
        } catch (Exception ex) {
            dashboardRemote = null;
            Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, "Service Unavailable",
                            "Dashboard service is not available.",
                            ex.getMessage()));
        }

        currentAdminUserId = Session.getCurrentUser() == null ? null : Session.getCurrentUser().getUserId();

        setupCharts();
        setupTables();
        if (dashboardRemote != null) {
            loadDashboard();
        }
    }

    @FXML
    private void onRefresh() {
        loadDashboard();
    }

    private void setupCharts() {
        revenueChart.setLegendVisible(false);
        revenueXAxis.setLabel("Date");
        revenueYAxis.setLabel("Revenue");
        bookingStatusChart.setLegendVisible(true);
    }

    private void setupTables() {
        topRoutesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        recentBookingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        topRoutesTable.setItems(topRoutesData);
        recentBookingsTable.setItems(recentBookingsData);

        topRoutesTable.setPlaceholder(new Label("No routes found."));
        recentBookingsTable.setPlaceholder(new Label("No recent bookings."));

        topRouteNoColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(""));
        topRouteNoColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    return;
                }
                setText(String.valueOf(getIndex() + 1));
            }
        });
        topRouteLabelColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safe(data.getValue().getRouteLabel())));
        topRouteBookingsColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getTotalBookings())));
        topRouteRevenueColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatMoney(data.getValue().getTotalRevenue())));

        bookingIdColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeId(data.getValue().getBookingId())));
        bookingPassengerColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safe(data.getValue().getPassengerName())));
        bookingRouteColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(routeLabel(data.getValue())));
        bookingTravelDateColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatDate(data.getValue().getTravelDate())));
        bookingStatusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeUpper(data.getValue().getStatus())));
        bookingTotalColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatMoney(data.getValue().getTotalPrice())));
        bookingBookedAtColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatDateTime(data.getValue().getBookingDate())));
    }

    private void loadDashboard() {
        if (dashboardRemote == null) {
            showAlert(Alert.AlertType.ERROR, "Unavailable", "Dashboard service is unavailable.", "Please reconnect and try again.");
            return;
        }
        if (currentAdminUserId == null) {
            showAlert(Alert.AlertType.ERROR, "Unauthorized", "You must be logged in as ADMIN.", "Please sign in again.");
            return;
        }

        setLoading(true);
        Task<DashboardResponseDTO> task = new Task<>() {
            @Override
            protected DashboardResponseDTO call() throws Exception {
                return dashboardRemote.getDashboardSummary(currentAdminUserId);
            }
        };

        task.setOnSucceeded(event -> Platform.runLater(() -> {
            applyDashboard(task.getValue());
            setLoading(false);
        }));

        task.setOnFailed(event -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            if (ex instanceof ValidationException validation) {
                showAlert(Alert.AlertType.WARNING, "Unauthorized", "You are not authorized to view this dashboard.", validation.getMessage());
            } else {
                showAlert(Alert.AlertType.ERROR, "Load Failed", "Unable to load dashboard.",
                        ex == null ? "Unexpected error." : ex.getMessage());
            }
            setLoading(false);
        }));

        startBackgroundTask(task, "admin-dashboard-load-task");
    }

    private void applyDashboard(DashboardResponseDTO response) {
        updateKpis(response == null ? List.of() : response.getKpis());
        updateRevenueChart(response == null ? List.of() : response.getRevenueTrend());
        updateBookingStatusChart(response == null ? Map.of() : response.getBookingStatusSummary());
        updateTopRoutes(response == null ? List.of() : response.getTopRoutes());
        updateRecentBookings(response == null ? List.of() : response.getRecentBookings());
        updateAlerts(response == null ? List.of() : response.getAlerts());

        LocalDateTime generatedAt = response == null ? null : response.getGeneratedAt();
        generatedAtLabel.setText(generatedAt == null ? "-" : "Updated: " + generatedAt.format(DATE_TIME_FMT).toLowerCase(Locale.ENGLISH));
    }

    private void updateKpis(List<KpiDTO> kpis) {
        if (kpis == null || kpis.size() < 6) {
            setKpi(kpiTitle1, kpiValue1, kpiSubtitle1, "-", "-", "-");
            setKpi(kpiTitle2, kpiValue2, kpiSubtitle2, "-", "-", "-");
            setKpi(kpiTitle3, kpiValue3, kpiSubtitle3, "-", "-", "-");
            setKpi(kpiTitle4, kpiValue4, kpiSubtitle4, "-", "-", "-");
            setKpi(kpiTitle5, kpiValue5, kpiSubtitle5, "-", "-", "-");
            setKpi(kpiTitle6, kpiValue6, kpiSubtitle6, "-", "-", "-");
            return;
        }
        setKpiFromDto(kpis.get(0), kpiTitle1, kpiValue1, kpiSubtitle1);
        setKpiFromDto(kpis.get(1), kpiTitle2, kpiValue2, kpiSubtitle2);
        setKpiFromDto(kpis.get(2), kpiTitle3, kpiValue3, kpiSubtitle3);
        setKpiFromDto(kpis.get(3), kpiTitle4, kpiValue4, kpiSubtitle4);
        setKpiFromDto(kpis.get(4), kpiTitle5, kpiValue5, kpiSubtitle5);
        setKpiFromDto(kpis.get(5), kpiTitle6, kpiValue6, kpiSubtitle6);
    }

    private void updateRevenueChart(List<ChartPointDTO> points) {
        revenueChart.getData().clear();
        if (points == null || points.isEmpty()) {
            return;
        }
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        for (ChartPointDTO point : points) {
            String label = point.getLabel() == null ? "-" : point.getLabel();
            series.getData().add(new javafx.scene.chart.XYChart.Data<>(label, point.getValue()));
        }
        revenueChart.getData().add(series);
    }

    private void updateBookingStatusChart(Map<String, Long> summary) {
        bookingStatusChart.getData().clear();
        if (summary == null || summary.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Long> entry : summary.entrySet()) {
            bookingStatusChart.getData().add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }
    }

    private void updateTopRoutes(List<RoutePopularityRowDTO> routes) {
        topRoutesData.setAll(routes == null ? List.of() : routes);
    }

    private void updateRecentBookings(List<BookingDTO> bookings) {
        recentBookingsData.setAll(bookings == null ? List.of() : bookings);
    }

    private void updateAlerts(List<String> alerts) {
        alertsBox.getChildren().clear();
        if (alerts == null || alerts.isEmpty()) {
            Label empty = new Label("No alerts.");
            empty.getStyleClass().add("alert-item");
            alertsBox.getChildren().add(empty);
            return;
        }
        for (String alert : alerts) {
            Label label = new Label(safe(alert));
            label.getStyleClass().add("alert-item");
            alertsBox.getChildren().add(label);
        }
    }

    private void setLoading(boolean value) {
        loading = value;
        if (refreshButton != null) {
            refreshButton.setDisable(value);
        }
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(value);
            loadingOverlay.setManaged(value);
        }
    }

    private void setKpiFromDto(KpiDTO kpi, Label title, Label value, Label subtitle) {
        if (kpi == null) {
            setKpi(title, value, subtitle, "-", "-", "-");
            return;
        }
        setKpi(title, value, subtitle, safe(kpi.getLabel()), safe(kpi.getValue()), safe(kpi.getSubtitle()));
    }

    private void setKpi(Label title, Label value, Label subtitle, String t, String v, String s) {
        title.setText(t);
        value.setText(v);
        subtitle.setText(s);
    }

    private String routeLabel(BookingDTO booking) {
        if (booking == null) {
            return "-";
        }
        String origin = safe(booking.getOriginCity());
        String destination = safe(booking.getDestinationCity());
        return origin + " -> " + destination;
    }

    private String formatMoney(Double value) {
        if (value == null) {
            return "-";
        }
        return MONEY_FORMAT.format(value);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FMT);
    }

    private String formatDateTime(LocalDateTime date) {
        return date == null ? "-" : date.format(DATE_TIME_FMT).toLowerCase(Locale.ENGLISH);
    }

    private String safeId(Long value) {
        return value == null ? "-" : String.valueOf(value);
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
