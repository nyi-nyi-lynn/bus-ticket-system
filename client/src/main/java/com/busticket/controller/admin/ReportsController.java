
package com.busticket.controller.admin;

import com.busticket.dto.BookingRowDTO;
import com.busticket.dto.ChartPointDTO;
import com.busticket.dto.CustomerActivityRowDTO;
import com.busticket.dto.ReportFilterDTO;
import com.busticket.dto.ReportResponseDTO;
import com.busticket.dto.RevenueRowDTO;
import com.busticket.dto.RoutePopularityRowDTO;
import com.busticket.dto.TripDTO;
import com.busticket.dto.TripPerformanceRowDTO;
import com.busticket.dto.BusDTO;
import com.busticket.dto.RouteDTO;
import com.busticket.exception.ValidationException;
import com.busticket.remote.BusRemote;
import com.busticket.remote.ReportRemote;
import com.busticket.remote.RouteRemote;
import com.busticket.remote.TripRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import com.busticket.util.PdfExportSupport;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReportsController {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat PCT_FMT = new DecimalFormat("0.0");

    @FXML private ComboBox<ReportType> reportTypeFilter;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<Option> routeFilter;
    @FXML private ComboBox<Option> busFilter;
    @FXML private ComboBox<Option> tripFilter;
    @FXML private ComboBox<String> bookingStatusFilter;
    @FXML private ComboBox<String> paymentStatusFilter;
    @FXML private Button generateButton;
    @FXML private Button exportCsvButton;
    @FXML private Button exportPdfButton;

    @FXML private Label kpiTitle1;
    @FXML private Label kpiValue1;
    @FXML private Label kpiSubtitle1;
    @FXML private Label kpiTitle2;
    @FXML private Label kpiValue2;
    @FXML private Label kpiSubtitle2;
    @FXML private Label kpiTitle3;
    @FXML private Label kpiValue3;
    @FXML private Label kpiSubtitle3;

    @FXML private BarChart<String, Number> reportChart;
    @FXML private CategoryAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;
    @FXML private TableView<Object> reportsTable;
    @FXML private Label generatedAtLabel;
    @FXML private Button toggleDetailsButton;

    private ReportRemote reportRemote;
    private RouteRemote routeRemote;
    private BusRemote busRemote;
    private TripRemote tripRemote;
    private Long currentAdminUserId;

    private final ObservableList<Object> tableData = FXCollections.observableArrayList();
    private ReportType currentReportType;
    private ReportResponseDTO lastResponse;
    private boolean loading;

    @FXML
    private void initialize() {
        try {
            reportRemote = RMIClient.getReportRemote();
            routeRemote = RMIClient.getRouteRemote();
            busRemote = RMIClient.getBusRemote();
            tripRemote = RMIClient.getTripRemote();
        } catch (Exception ex) {
            reportRemote = null;
            routeRemote = null;
            busRemote = null;
            tripRemote = null;
            Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, "Service Unavailable",
                            "Reports service is not available.",
                            ex.getMessage()));
        }

        currentAdminUserId = Session.getCurrentUser() != null ? Session.getCurrentUser().getUserId() : null;

        setupFilters();
        setupTable();
        setupChart();
        if (reportRemote != null) {
            loadFilterLookups();
        }
        updateGenerateState();
    }

    private void setupFilters() {
        reportTypeFilter.setItems(FXCollections.observableArrayList(ReportType.values()));
        reportTypeFilter.setValue(ReportType.REVENUE_SUMMARY);
        currentReportType = ReportType.REVENUE_SUMMARY;

        LocalDate today = LocalDate.now();
        fromDatePicker.setValue(today.minusDays(30));
        toDatePicker.setValue(today);

        bookingStatusFilter.setItems(FXCollections.observableArrayList(List.of("ALL", "PENDING", "CONFIRMED", "CANCELLED")));
        bookingStatusFilter.setValue("ALL");

        paymentStatusFilter.setItems(FXCollections.observableArrayList(List.of("ALL", "PENDING", "PAID", "FAILED")));
        paymentStatusFilter.setValue("ALL");

        reportTypeFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentReportType = newVal == null ? ReportType.REVENUE_SUMMARY : newVal;
            updateGenerateState();
            clearReport();
        });
        fromDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateGenerateState());
        toDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateGenerateState());
        routeFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateGenerateState());
        busFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateGenerateState());
        tripFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateGenerateState());
        bookingStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateGenerateState());
        paymentStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateGenerateState());
    }

    private void setupTable() {
        reportsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        reportsTable.setItems(tableData);
        reportsTable.setPlaceholder(new Label("No data"));
    }

    @FXML
    private void onToggleDetails() {
        if (reportsTable == null) {
            return;
        }
        openDetailsModal();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void openDetailsModal() {
        TableView<Object> modalTable = new TableView<>();
        modalTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        modalTable.setItems(tableData);
        modalTable.setPlaceholder(new Label("No data"));

        for (TableColumn column : reportsTable.getColumns()) {
            TableColumn<Object, Object> clone = new TableColumn<>(column.getText());
            if (column.getCellValueFactory() != null) {
                clone.setCellValueFactory((javafx.util.Callback) column.getCellValueFactory());
            }
            if (column.getCellFactory() != null) {
                clone.setCellFactory((javafx.util.Callback) column.getCellFactory());
            }
            clone.setSortable(column.isSortable());
            clone.setPrefWidth(column.getWidth());
            modalTable.getColumns().add(clone);
        }

        Label title = new Label("Report Details");
        title.getStyleClass().add("section-title");

        Button closeButton = new Button("Close");
        closeButton.getStyleClass().add("primary-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, title, spacer, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(12, header, modalTable);
        root.setPadding(new Insets(16));
        root.setPrefSize(1100, 700);
        root.getStyleClass().add("card");

        Scene scene = new Scene(root, 1100, 700);
        if (getClass().getResource("/com/busticket/view/theme.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/busticket/view/theme.css").toExternalForm());
        }

        Stage dialog = new Stage();
        dialog.setTitle("Report Details");
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (reportsTable.getScene() != null && reportsTable.getScene().getWindow() != null) {
            dialog.initOwner(reportsTable.getScene().getWindow());
        }
        dialog.setResizable(true);
        dialog.setScene(scene);

        closeButton.setOnAction(event -> dialog.close());
        dialog.showAndWait();
    }

    private void setupChart() {
        reportChart.setLegendVisible(false);
        chartXAxis.setLabel("Label");
        chartYAxis.setLabel("Value");
    }
    private void loadFilterLookups() {
        if (routeRemote == null || busRemote == null || tripRemote == null) {
            return;
        }

        Task<LookupData> lookupTask = new Task<>() {
            @Override
            protected LookupData call() throws Exception {
                List<RouteDTO> routes = routeRemote.getAllRoutes();
                List<BusDTO> buses = busRemote.getAllBuses();
                List<TripDTO> trips = tripRemote.getAllTrips();
                return new LookupData(routes, buses, trips);
            }
        };

        lookupTask.setOnSucceeded(event -> Platform.runLater(() -> {
            LookupData data = lookupTask.getValue();
            routeFilter.setItems(FXCollections.observableArrayList(buildRouteOptions(data.routes())));
            busFilter.setItems(FXCollections.observableArrayList(buildBusOptions(data.buses())));
            tripFilter.setItems(FXCollections.observableArrayList(buildTripOptions(data.trips())));

            routeFilter.setValue(Option.all("All Routes"));
            busFilter.setValue(Option.all("All Buses"));
            tripFilter.setValue(Option.all("All Trips"));
        }));

        lookupTask.setOnFailed(event -> Platform.runLater(() ->
                showAlert(Alert.AlertType.ERROR, "Load Failed", "Unable to load filters.",
                        lookupTask.getException() == null ? "Unexpected error." : lookupTask.getException().getMessage())));

        startBackgroundTask(lookupTask, "reports-lookup-task");
    }

    @FXML
    private void onGenerate() {
        if (reportRemote == null) {
            showAlert(Alert.AlertType.ERROR, "Unavailable", "Report service is unavailable.", "Please reconnect and try again.");
            return;
        }
        if (currentAdminUserId == null) {
            showAlert(Alert.AlertType.ERROR, "Unauthorized", "You must be logged in as ADMIN.", "Please sign in again.");
            return;
        }
        if (isInvalidDateRange()) {
            showAlert(Alert.AlertType.WARNING, "Invalid Dates", "Please select a valid date range.", "From date must be before To date.");
            return;
        }

        ReportFilterDTO filter = buildFilter();
        setLoading(true);

        Task<ReportResponseDTO> task = new Task<>() {
            @Override
            protected ReportResponseDTO call() throws Exception {
                return switch (currentReportType) {
                    case REVENUE_SUMMARY -> reportRemote.getRevenueReport(filter);
                    case BOOKING_SUMMARY -> reportRemote.getBookingReport(filter);
                    case TRIP_PERFORMANCE -> reportRemote.getTripPerformanceReport(filter);
                    case ROUTE_POPULARITY -> reportRemote.getRoutePopularityReport(filter);
                    case CUSTOMER_ACTIVITY -> reportRemote.getCustomerActivityReport(filter);
                };
            }
        };

        task.setOnSucceeded(event -> Platform.runLater(() -> {
            ReportResponseDTO response = task.getValue();
            applyReport(response);
            setLoading(false);
        }));

        task.setOnFailed(event -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            if (ex instanceof ValidationException validation) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Invalid report filters.", validation.getMessage());
            } else {
                showAlert(Alert.AlertType.ERROR, "Report Failed", "Unable to generate report.",
                        ex == null ? "Unexpected error." : ex.getMessage());
            }
            setLoading(false);
        }));

        startBackgroundTask(task, "reports-generate-task");
    }

    @FXML
    private void onExportCsv() {
        if (lastResponse == null || tableData.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Data", "Nothing to export.", "Generate a report first.");
            return;
        }

        Window window = reportsTable.getScene() == null ? null : reportsTable.getScene().getWindow();
        File target = PdfExportSupport.chooseExportTarget(
                window,
                "Export CSV",
                "csv",
                currentReportType.filePrefix() + "_" + formatDate(LocalDate.now())
        );
        if (target == null) {
            return;
        }

        try {
            writeCsv(target);
            showAlert(Alert.AlertType.INFORMATION, "Exported", "CSV export completed.", target.getAbsolutePath());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Export Failed", "Unable to export CSV.", ex.getMessage());
        }
    }

    @FXML
    private void onExportPdf() {
        if (lastResponse == null || tableData.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Data", "Nothing to export.", "Generate a report first.");
            return;
        }

        Window window = reportsTable.getScene() == null ? null : reportsTable.getScene().getWindow();
        File target = PdfExportSupport.chooseExportTarget(
                window,
                "Export PDF",
                "pdf",
                currentReportType.filePrefix() + "_" + formatDate(LocalDate.now())
        );
        if (target == null) {
            return;
        }

        try {
            writePdf(target);
            showAlert(Alert.AlertType.INFORMATION, "Exported", "PDF export completed.", target.getAbsolutePath());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Export Failed", "Unable to export PDF.", ex.getMessage());
        }
    }

    private void applyReport(ReportResponseDTO response) {
        lastResponse = response;
        updateKpis(response == null ? List.of() : response.getKpis());
        updateChart(response == null ? List.of() : response.getChartData());
        configureColumns();

        tableData.clear();
        if (response != null && response.getTableRows() != null) {
            tableData.addAll(response.getTableRows());
        }

        generatedAtLabel.setText(response == null || response.getGeneratedAt() == null
                ? "-"
                : "Generated: " + response.getGeneratedAt().format(DATE_TIME_FMT));

        exportCsvButton.setDisable(tableData.isEmpty() || loading);
        exportPdfButton.setDisable(tableData.isEmpty() || loading);
    }

    private void configureColumns() {
        List<TableColumn<Object, String>> columns = new ArrayList<>();
        columns.add(createNoColumn());

        switch (currentReportType) {
            case REVENUE_SUMMARY -> {
                columns.add(textColumn("Date", row -> formatDate(((RevenueRowDTO) row).getDate())));
                columns.add(textColumn("Total Revenue", row -> formatMoney(((RevenueRowDTO) row).getTotalRevenue())));
                columns.add(textColumn("Total Bookings", row -> String.valueOf(((RevenueRowDTO) row).getTotalBookings())));
            }
            case BOOKING_SUMMARY -> {
                columns.add(textColumn("Status", row -> safeUpper(((BookingRowDTO) row).getBookingStatus())));
                columns.add(textColumn("Total Bookings", row -> String.valueOf(((BookingRowDTO) row).getTotalBookings())));
                columns.add(textColumn("Total Revenue", row -> formatMoney(((BookingRowDTO) row).getTotalRevenue())));
            }
            case TRIP_PERFORMANCE -> {
                columns.add(textColumn("Trip ID", row -> safeString(((TripPerformanceRowDTO) row).getTripId())));
                columns.add(textColumn("Route", row -> safe(((TripPerformanceRowDTO) row).getRouteLabel())));
                columns.add(textColumn("Bus", row -> safe(((TripPerformanceRowDTO) row).getBusNumber())));
                columns.add(textColumn("Travel Date", row -> formatDate(((TripPerformanceRowDTO) row).getTravelDate())));
                columns.add(textColumn("Total Seats", row -> String.valueOf(((TripPerformanceRowDTO) row).getTotalSeats())));
                columns.add(textColumn("Sold Seats", row -> String.valueOf(((TripPerformanceRowDTO) row).getSoldSeats())));
                columns.add(textColumn("Occupancy", row -> PCT_FMT.format(((TripPerformanceRowDTO) row).getOccupancyRate() * 100.0) + "%"));
                columns.add(textColumn("Revenue", row -> formatMoney(((TripPerformanceRowDTO) row).getTotalRevenue())));
            }
            case ROUTE_POPULARITY -> {
                columns.add(textColumn("Route", row -> safe(((RoutePopularityRowDTO) row).getRouteLabel())));
                columns.add(textColumn("Bookings", row -> String.valueOf(((RoutePopularityRowDTO) row).getTotalBookings())));
                columns.add(textColumn("Revenue", row -> formatMoney(((RoutePopularityRowDTO) row).getTotalRevenue())));
            }
            case CUSTOMER_ACTIVITY -> {
                columns.add(textColumn("Customer", row -> safe(((CustomerActivityRowDTO) row).getName())));
                columns.add(textColumn("Email", row -> safe(((CustomerActivityRowDTO) row).getEmail())));
                columns.add(textColumn("Bookings", row -> String.valueOf(((CustomerActivityRowDTO) row).getTotalBookings())));
                columns.add(textColumn("Total Spent", row -> formatMoney(((CustomerActivityRowDTO) row).getTotalSpent())));
            }
        }

        reportsTable.getColumns().setAll(columns);
    }

    private TableColumn<Object, String> createNoColumn() {
        TableColumn<Object, String> column = new TableColumn<>("No.");
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(""));
        column.setCellFactory(col -> new TableCell<>() {
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
        return column;
    }

    private TableColumn<Object, String> textColumn(String title, ValueExtractor extractor) {
        TableColumn<Object, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(extractor.get(data.getValue())));
        return column;
    }

    private void updateKpis(List<com.busticket.dto.KpiDTO> kpis) {
        if (kpis == null || kpis.size() < 3) {
            setKpi(kpiTitle1, kpiValue1, kpiSubtitle1, "-", "-", "-");
            setKpi(kpiTitle2, kpiValue2, kpiSubtitle2, "-", "-", "-");
            setKpi(kpiTitle3, kpiValue3, kpiSubtitle3, "-", "-", "-");
            return;
        }
        setKpiFromDto(kpis.get(0), kpiTitle1, kpiValue1, kpiSubtitle1);
        setKpiFromDto(kpis.get(1), kpiTitle2, kpiValue2, kpiSubtitle2);
        setKpiFromDto(kpis.get(2), kpiTitle3, kpiValue3, kpiSubtitle3);
    }

    private void setKpiFromDto(com.busticket.dto.KpiDTO kpi, Label title, Label value, Label subtitle) {
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

    private void updateChart(List<ChartPointDTO> points) {
        reportChart.getData().clear();
        if (points == null || points.isEmpty()) {
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (ChartPointDTO point : points) {
            String label = point.getLabel() == null ? "-" : point.getLabel();
            series.getData().add(new XYChart.Data<>(label, point.getValue()));
        }
        reportChart.getData().add(series);
    }

    private void clearReport() {
        tableData.clear();
        reportChart.getData().clear();
        generatedAtLabel.setText("-");
        updateKpis(List.of());
        exportCsvButton.setDisable(true);
        exportPdfButton.setDisable(true);
    }
    private ReportFilterDTO buildFilter() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setFromDate(fromDatePicker.getValue());
        filter.setToDate(toDatePicker.getValue());
        filter.setRouteId(resolveOptionId(routeFilter.getValue()));
        filter.setBusId(resolveOptionId(busFilter.getValue()));
        filter.setTripId(resolveOptionId(tripFilter.getValue()));
        filter.setBookingStatus(normalizeFilterValue(bookingStatusFilter.getValue()));
        filter.setPaymentStatus(normalizeFilterValue(paymentStatusFilter.getValue()));
        filter.setRequestedByUserId(currentAdminUserId);
        return filter;
    }

    private Long resolveOptionId(Option option) {
        if (option == null || option.id() == null) {
            return null;
        }
        return option.id();
    }

    private String normalizeFilterValue(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private List<Option> buildRouteOptions(List<RouteDTO> routes) {
        List<Option> options = new ArrayList<>();
        options.add(Option.all("All Routes"));
        if (routes != null) {
            for (RouteDTO route : routes) {
                if (route == null || route.getRouteId() == null) {
                    continue;
                }
                String label = safe(route.getOriginCity()) + " -> " + safe(route.getDestinationCity());
                options.add(new Option(route.getRouteId(), label));
            }
        }
        return options;
    }

    private List<Option> buildBusOptions(List<BusDTO> buses) {
        List<Option> options = new ArrayList<>();
        options.add(Option.all("All Buses"));
        if (buses != null) {
            for (BusDTO bus : buses) {
                if (bus == null || bus.getBusId() == null) {
                    continue;
                }
                String label = safe(bus.getBusNumber());
                if (bus.getBusName() != null && !bus.getBusName().isBlank()) {
                    label = label + " - " + bus.getBusName().trim();
                }
                options.add(new Option(bus.getBusId(), label));
            }
        }
        return options;
    }

    private List<Option> buildTripOptions(List<TripDTO> trips) {
        List<Option> options = new ArrayList<>();
        options.add(Option.all("All Trips"));
        if (trips != null) {
            for (TripDTO trip : trips) {
                if (trip == null || trip.getTripId() == null) {
                    continue;
                }
                String label = safe(trip.getOriginCity()) + " -> " + safe(trip.getDestinationCity());
                if (trip.getTravelDate() != null) {
                    label += " | " + trip.getTravelDate().format(DATE_FMT);
                }
                options.add(new Option(trip.getTripId(), label));
            }
        }
        return options;
    }

    private void setLoading(boolean value) {
        loading = value;
        generateButton.setDisable(value || isInvalidDateRange());
        exportCsvButton.setDisable(value || tableData.isEmpty());
        exportPdfButton.setDisable(value || tableData.isEmpty());
        reportTypeFilter.setDisable(value);
        fromDatePicker.setDisable(value);
        toDatePicker.setDisable(value);
        routeFilter.setDisable(value);
        busFilter.setDisable(value);
        tripFilter.setDisable(value);
        bookingStatusFilter.setDisable(value);
        paymentStatusFilter.setDisable(value);
    }

    private void updateGenerateState() {
        generateButton.setDisable(loading || isInvalidDateRange());
    }

    private boolean isInvalidDateRange() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        return from == null || to == null || from.isAfter(to);
    }

    private void writeCsv(File file) throws IOException {
        List<String> headers = buildHeaders();
        List<List<String>> rows = buildRows();

        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(String.join(",", headers));
            writer.newLine();
            for (List<String> row : rows) {
                writer.write(row.stream().map(this::escapeCsv).collect(Collectors.joining(",")));
                writer.newLine();
            }
        }
    }

    private void writePdf(File file) throws IOException {
        List<String> headers = buildHeaders();
        List<List<String>> rows = buildRows();

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            float margin = 50f;
            float y = page.getMediaBox().getHeight() - margin;
            float lineHeight = 14f;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setFont(fontBold, 16);
                PdfExportSupport.writeText(content, margin, y, currentReportType.label());
                y -= lineHeight * 1.5f;

                content.setFont(fontRegular, 11);
                PdfExportSupport.writeText(content, margin, y, "Date Range: " + formatDate(fromDatePicker.getValue()) + " to " + formatDate(toDatePicker.getValue()));
                y -= lineHeight;

                String filters = buildFilterSummary();
                PdfExportSupport.writeText(content, margin, y, "Filters: " + filters);
                y -= lineHeight * 1.5f;

                List<String> kpiLines = List.of(
                        kpiTitle1.getText() + ": " + kpiValue1.getText(),
                        kpiTitle2.getText() + ": " + kpiValue2.getText(),
                        kpiTitle3.getText() + ": " + kpiValue3.getText()
                );
                for (String line : kpiLines) {
                    PdfExportSupport.writeText(content, margin, y, line);
                    y -= lineHeight;
                }

                y -= lineHeight * 0.5f;
                content.setFont(fontBold, 11);
                PdfExportSupport.writeText(content, margin, y, String.join(" | ", headers));
                y -= lineHeight;

                content.setFont(fontRegular, 10);
                for (List<String> row : rows) {
                    if (y < margin) {
                        break;
                    }
                    PdfExportSupport.writeText(content, margin, y, String.join(" | ", row));
                    y -= lineHeight;
                }
            }

            document.save(file);
        }
    }

    private List<String> buildHeaders() {
        return switch (currentReportType) {
            case REVENUE_SUMMARY -> List.of("Date", "Total Revenue", "Total Bookings");
            case BOOKING_SUMMARY -> List.of("Status", "Total Bookings", "Total Revenue");
            case TRIP_PERFORMANCE -> List.of("Trip ID", "Route", "Bus", "Travel Date", "Total Seats", "Sold Seats", "Occupancy", "Revenue");
            case ROUTE_POPULARITY -> List.of("Route", "Bookings", "Revenue");
            case CUSTOMER_ACTIVITY -> List.of("Customer", "Email", "Bookings", "Total Spent");
        };
    }

    private List<List<String>> buildRows() {
        List<List<String>> rows = new ArrayList<>();
        for (Object row : tableData) {
            rows.add(buildRowValues(row));
        }
        return rows;
    }

    private List<String> buildRowValues(Object row) {
        return switch (currentReportType) {
            case REVENUE_SUMMARY -> {
                RevenueRowDTO dto = (RevenueRowDTO) row;
                yield List.of(formatDate(dto.getDate()), formatMoney(dto.getTotalRevenue()), String.valueOf(dto.getTotalBookings()));
            }
            case BOOKING_SUMMARY -> {
                BookingRowDTO dto = (BookingRowDTO) row;
                yield List.of(safeUpper(dto.getBookingStatus()), String.valueOf(dto.getTotalBookings()), formatMoney(dto.getTotalRevenue()));
            }
            case TRIP_PERFORMANCE -> {
                TripPerformanceRowDTO dto = (TripPerformanceRowDTO) row;
                yield List.of(
                        safeString(dto.getTripId()),
                        safe(dto.getRouteLabel()),
                        safe(dto.getBusNumber()),
                        formatDate(dto.getTravelDate()),
                        String.valueOf(dto.getTotalSeats()),
                        String.valueOf(dto.getSoldSeats()),
                        PCT_FMT.format(dto.getOccupancyRate() * 100.0) + "%",
                        formatMoney(dto.getTotalRevenue())
                );
            }
            case ROUTE_POPULARITY -> {
                RoutePopularityRowDTO dto = (RoutePopularityRowDTO) row;
                yield List.of(safe(dto.getRouteLabel()), String.valueOf(dto.getTotalBookings()), formatMoney(dto.getTotalRevenue()));
            }
            case CUSTOMER_ACTIVITY -> {
                CustomerActivityRowDTO dto = (CustomerActivityRowDTO) row;
                yield List.of(safe(dto.getName()), safe(dto.getEmail()), String.valueOf(dto.getTotalBookings()), formatMoney(dto.getTotalSpent()));
            }
        };
    }

    private String buildFilterSummary() {
        List<String> parts = new ArrayList<>();
        parts.add(optionLabel(routeFilter.getValue(), "All Routes"));
        parts.add(optionLabel(busFilter.getValue(), "All Buses"));
        parts.add(optionLabel(tripFilter.getValue(), "All Trips"));
        parts.add("Booking: " + safe(bookingStatusFilter.getValue()));
        parts.add("Payment: " + safe(paymentStatusFilter.getValue()));
        return parts.stream().filter(Objects::nonNull).collect(Collectors.joining(", "));
    }

    private String optionLabel(Option option, String fallback) {
        if (option == null) {
            return fallback;
        }
        return option.label();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String formatMoney(double value) {
        return MONEY_FMT.format(value);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FMT);
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

    private String safeString(Long value) {
        return value == null ? "-" : String.valueOf(value);
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

    private interface ValueExtractor {
        String get(Object row);
    }

    private record LookupData(List<RouteDTO> routes, List<BusDTO> buses, List<TripDTO> trips) {
    }

    private record Option(Long id, String label) {
        static Option all(String label) {
            return new Option(null, label);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private enum ReportType {
        REVENUE_SUMMARY("Revenue Summary", "revenue"),
        BOOKING_SUMMARY("Booking Summary", "booking"),
        TRIP_PERFORMANCE("Trip Performance", "trip_performance"),
        ROUTE_POPULARITY("Route Popularity", "route_popularity"),
        CUSTOMER_ACTIVITY("Customer Activity", "customer_activity");

        private final String label;
        private final String filePrefix;

        ReportType(String label, String filePrefix) {
            this.label = label;
            this.filePrefix = filePrefix;
        }

        public String label() {
            return label;
        }

        public String filePrefix() {
            return filePrefix;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
