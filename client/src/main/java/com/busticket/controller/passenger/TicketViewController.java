package com.busticket.controller.passenger;

import com.busticket.dto.TicketDetailsDTO;
import com.busticket.enums.BookingStatus;
import com.busticket.enums.PaymentStatus;
import com.busticket.remote.TicketRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import com.busticket.util.PdfExportSupport;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class TicketViewController {
    private static final String BUS_COMPANY_NAME = "Myanmar Express Bus";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0");

    @FXML private Label companyNameLabel;
    @FXML private Label ticketCodeLabel;
    @FXML private Label routeLabel;
    @FXML private Label travelDateLabel;
    @FXML private Label departureTimeLabel;
    @FXML private Label arrivalTimeLabel;
    @FXML private Label busInfoLabel;
    @FXML private Label passengerNameLabel;
    @FXML private Label seatNumbersLabel;
    @FXML private Label totalPriceLabel;
    @FXML private Label bookingStatusLabel;
    @FXML private Label paymentStatusLabel;
    @FXML private VBox ticketCard;

    private Long bookingId;
    private TicketDetailsDTO ticketDetails;

    @FXML
    private void initialize() {
        if (companyNameLabel != null) {
            companyNameLabel.setText(BUS_COMPANY_NAME);
        }
        clearLabels();
        loadTicketFromSession();
    }

    @FXML
    private void downloadPdf() {
        if (ticketDetails == null && isBlank(ticketCodeLabel)) {
            showAlert(Alert.AlertType.WARNING, "No Ticket", "Ticket data is missing.", "Please load a booking first.");
            return;
        }

        String code = safe(getLabelText(ticketCodeLabel));
        String safeCode = sanitizeFilePart(code);
        String initialFileName = !"-".equals(safeCode) ? "Ticket_" + safeCode : "Ticket";

        Window window = ticketCard != null && ticketCard.getScene() != null ? ticketCard.getScene().getWindow() : null;
        File target = PdfExportSupport.chooseExportTarget(window, "Save Ticket PDF", "pdf", initialFileName);
        if (target == null) {
            return;
        }

        try {
            writePdf(target);
            showAlert(Alert.AlertType.INFORMATION, "PDF Saved", "Ticket PDF generated successfully.", target.getAbsolutePath());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "PDF Error", "Unable to generate PDF.", ex.getMessage());
        }
    }

    @FXML
    private void printTicket() {
        if (ticketCard == null) {
            showAlert(Alert.AlertType.ERROR, "Print Error", "Ticket layout not available.", "Please try again.");
            return;
        }
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showAlert(Alert.AlertType.ERROR, "Print Error", "No printer available.", "Please check printer settings.");
            return;
        }
        Window window = ticketCard.getScene() != null ? ticketCard.getScene().getWindow() : null;
        if (job.showPrintDialog(window)) {
            boolean printed = job.printPage(ticketCard);
            if (printed) {
                job.endJob();
            }
        }
    }

    @FXML
    private void onBack() {
        if (Session.isGuest()) {
            SceneSwitcher.switchContent("/com/busticket/view/passenger/SearchTripsView.fxml");
        } else {
            SceneSwitcher.switchContent("/com/busticket/view/passenger/SearchTripsView.fxml");
        }
    }

    private void loadTicketFromSession() {
        if (bookingId != null) {
            loadTicketDetails();
            return;
        }
        Long sessionBookingId = Session.getCurrentBookingId();
        if (sessionBookingId != null) {
            bookingId = sessionBookingId;
            loadTicketDetails();
        }
    }

    private void loadTicketDetails() {
        if (bookingId == null) {
            return;
        }
        try {
            TicketRemote ticketRemote = RMIClient.getTicketRemote();
            TicketDetailsDTO details = ticketRemote.getTicketDetailsByBookingId(bookingId);
            if (details == null) {
                showAlert(Alert.AlertType.WARNING, "Ticket Not Found", "Unable to locate this ticket.", "Please refresh and try again.");
                clearLabels();
                return;
            }
            ticketDetails = details;
            populateLabels(details);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Load Failed", "Unable to load ticket details.", ex.getMessage());
        }
    }

    private void populateLabels(TicketDetailsDTO details) {
        if (details == null) {
            clearLabels();
            return;
        }

        setLabelValue(companyNameLabel, BUS_COMPANY_NAME);
        setLabelValue(ticketCodeLabel, safe(details.getTicketCode()));
        setLabelValue(routeLabel, formatRoute(details.getOriginCity(), details.getDestinationCity()));
        setLabelValue(travelDateLabel, formatDate(details.getTravelDate()));
        setLabelValue(departureTimeLabel, formatTime(details.getDepartureTime()));
        setLabelValue(arrivalTimeLabel, formatTime(details.getArrivalTime()));
        setLabelValue(busInfoLabel, formatBusInfo(details.getBusNumber(), details.getBusType()));
        setLabelValue(passengerNameLabel, safe(details.getPassengerName()));
        setLabelValue(seatNumbersLabel, formatSeats(details.getSeatNumbers()));
        setLabelValue(totalPriceLabel, formatPrice(details.getTotalPrice()));

        BookingStatus bookingStatus = details.getBookingStatus();
        setLabelValue(bookingStatusLabel, bookingStatus != null ? formatStatus(bookingStatus.name()) : "-");
        setBookingStatusColor(bookingStatusLabel, bookingStatus);

        PaymentStatus paymentStatus = details.getPaymentStatus();
        setLabelValue(paymentStatusLabel, paymentStatus != null ? formatStatus(paymentStatus.name()) : "NOT PAID");
    }

    private void clearLabels() {
        setLabelValue(ticketCodeLabel, "-");
        setLabelValue(routeLabel, "-");
        setLabelValue(travelDateLabel, "-");
        setLabelValue(departureTimeLabel, "-");
        setLabelValue(arrivalTimeLabel, "-");
        setLabelValue(busInfoLabel, "-");
        setLabelValue(passengerNameLabel, "-");
        setLabelValue(seatNumbersLabel, "-");
        setLabelValue(totalPriceLabel, "-");
        setLabelValue(bookingStatusLabel, "-");
        setLabelValue(paymentStatusLabel, "-");
        setBookingStatusColor(bookingStatusLabel, null);
    }

    private void setBookingStatusColor(Label label, BookingStatus status) {
        if (label == null) {
            return;
        }
        if (status == null) {
            label.setTextFill(Color.web("#333333"));
            return;
        }
        switch (status) {
            case CANCELLED -> label.setTextFill(Color.web("#c0392b"));
            case PENDING -> label.setTextFill(Color.web("#e67e22"));
            case CONFIRMED -> label.setTextFill(Color.web("#27ae60"));
            default -> label.setTextFill(Color.web("#333333"));
        }
    }

    private String formatRoute(String origin, String destination) {
        String left = safe(origin);
        String right = safe(destination);
        if ("-".equals(left) && "-".equals(right)) {
            return "-";
        }
        return left + " \u2192 " + right;
    }

    private String formatBusInfo(String busNumber, String busType) {
        String number = safe(busNumber);
        String type = safe(busType);
        if ("-".equals(number) && "-".equals(type)) {
            return "-";
        }
        if ("-".equals(type)) {
            return number;
        }
        if ("-".equals(number)) {
            return type;
        }
        return number + " (" + type + ")";
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMAT.format(date);
    }

    private String formatTime(LocalTime time) {
        return time == null ? "-" : TIME_FORMAT.format(time).toLowerCase(Locale.ENGLISH);
    }

    private String formatSeats(List<String> seats) {
        if (seats == null || seats.isEmpty()) {
            return "-";
        }
        return String.join(", ", seats);
    }

    private String formatPrice(Double amount) {
        if (amount == null) {
            return "-";
        }
        return PRICE_FORMAT.format(amount) + " MMK";
    }

    private String formatStatus(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String lower = value.trim().toLowerCase(Locale.ENGLISH);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String safe(String value) {
        if (value == null) {
            return "-";
        }
        String text = value.trim();
        return text.isEmpty() ? "-" : text;
    }

    private void setLabelValue(Label label, String value) {
        if (label != null) {
            label.setText(value == null ? "-" : value);
        }
    }

    private boolean isBlank(Label label) {
        if (label == null) {
            return true;
        }
        String text = label.getText();
        return text == null || text.isBlank() || "-".equals(text.trim());
    }

    private String getLabelText(Label label) {
        return label == null ? "-" : label.getText();
    }

    private String sanitizeFilePart(String value) {
        if (value == null || value.isBlank() || "-".equals(value)) {
            return "-";
        }
        return value.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private void writePdf(File file) throws IOException {
        String companyName = safe(getLabelText(companyNameLabel));
        String code = safe(getLabelText(ticketCodeLabel));
        String route = safe(getLabelText(routeLabel));
        String travelDate = safe(getLabelText(travelDateLabel));
        String depart = safe(getLabelText(departureTimeLabel));
        String arrive = safe(getLabelText(arrivalTimeLabel));
        String busInfo = safe(getLabelText(busInfoLabel));
        String passenger = safe(getLabelText(passengerNameLabel));
        String seats = safe(getLabelText(seatNumbersLabel));
        String total = safe(getLabelText(totalPriceLabel));
        String bookingStatus = safe(getLabelText(bookingStatusLabel));
        String paymentStatus = safe(getLabelText(paymentStatusLabel));

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 50f;
            float cardWidth = pageWidth - (margin * 2);
            float cardHeight = 540f;
            float cardX = margin;
            float cardY = pageHeight - margin - cardHeight;

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setNonStrokingColor(1f, 1f, 1f);
                content.addRect(cardX, cardY, cardWidth, cardHeight);
                content.fill();
                content.setStrokingColor(220f / 255f, 220f / 255f, 220f / 255f);
                content.addRect(cardX, cardY, cardWidth, cardHeight);
                content.stroke();

                float x = cardX + 24f;
                float y = cardY + cardHeight - 36f;

                content.setFont(fontBold, 18);
                PdfExportSupport.writeText(content, x, y, companyName);

                y -= 18f;
                content.setFont(fontRegular, 11);
                PdfExportSupport.writeText(content, x, y, "Official E-Ticket");

                float codeBoxWidth = 200f;
                float codeBoxHeight = 24f;
                float codeX = cardX + cardWidth - codeBoxWidth - 24f;
                float codeY = cardY + cardHeight - 52f;

                content.setNonStrokingColor(1f, 242f / 255f, 204f / 255f);
                content.addRect(codeX, codeY, codeBoxWidth, codeBoxHeight);
                content.fill();
                content.setNonStrokingColor(0f, 0f, 0f);

                content.setFont(fontBold, 12);
                PdfExportSupport.writeText(content, codeX + 8f, codeY + 7f, "Ticket Code: " + code);

                y -= 40f;
                content.setFont(fontBold, 20);
                PdfExportSupport.writeText(content, x, y, route);

                y -= 26f;
                content.setStrokingColor(230f / 255f, 230f / 255f, 230f / 255f);
                content.moveTo(cardX + 16f, y);
                content.lineTo(cardX + cardWidth - 16f, y);
                content.stroke();

                y -= 24f;
                y = writeKeyValue(content, fontBold, fontRegular, x, y, "Travel Date", travelDate);
                y = writeKeyValue(content, fontBold, fontRegular, x, y, "Departure Time", depart);
                y = writeKeyValue(content, fontBold, fontRegular, x, y, "Arrival Time", arrive);
                y = writeKeyValue(content, fontBold, fontRegular, x, y, "Bus", busInfo);
                y -= 8f;

                y = writeKeyValue(content, fontBold, fontRegular, x, y, "Passenger Name", passenger);
                y = writeKeyValue(content, fontBold, fontRegular, x, y, "Seat Numbers", seats);
                y -= 8f;

                y = writeKeyValue(content, fontBold, fontRegular, x, y, "Total Price", total);
                y = writeKeyValue(content, fontBold, fontRegular, x, y, "Booking Status", bookingStatus);
                writeKeyValue(content, fontBold, fontRegular, x, y, "Payment Status", paymentStatus);
            }

            document.save(file);
        }
    }

    private float writeKeyValue(PDPageContentStream content, PDType1Font fontBold, PDType1Font fontRegular,
                                float x, float y, String label, String value) throws IOException {
        content.setFont(fontBold, 11);
        PdfExportSupport.writeText(content, x, y, label + ":");

        content.setFont(fontRegular, 11);
        PdfExportSupport.writeText(content, x + 160f, y, value);

        return y - 18f;
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
