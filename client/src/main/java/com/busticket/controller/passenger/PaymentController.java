package com.busticket.controller.passenger;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.PaymentDTO;
import com.busticket.dto.PaymentRequestDTO;
import com.busticket.dto.TripDTO;
import com.busticket.enums.PaymentMethod;
import com.busticket.enums.PaymentStatus;
import com.busticket.remote.PaymentRemote;
import com.busticket.remote.TripRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PaymentController {

    @FXML private Label routeValueLabel;
    @FXML private Label travelDateValueLabel;
    @FXML private Label departureTimeValueLabel;
    @FXML private Label selectedSeatsValueLabel;
    @FXML private Label totalPriceValueLabel;

    @FXML private ToggleGroup paymentMethodToggle;
    @FXML private RadioButton kbzPayRadio;
    @FXML private RadioButton wavePayRadio;
    @FXML private RadioButton cardRadio;

    @FXML private VBox qrSection;
    @FXML private ImageView qrImageView;
    @FXML private Label accountNameValueLabel;
    @FXML private Label accountPhoneValueLabel;
    @FXML private Label instructionLabel;
    @FXML private Button confirmPaymentButton;

    private static final String METHOD_KBZPAY = "KBZPAY";
    private static final String METHOD_WAVEPAY = "WAVEPAY";
    private static final String METHOD_CARD = "CARD";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private PaymentRemote paymentRemote;
    private BookingDTO bookingDTO;
    private TripDTO tripDTO;
    private List<String> selectedSeats = List.of();
    private double totalAmount;

    @FXML
    private void initialize() {
        initializePaymentMethods();
        initializeRemotes();
        renderBookingSummary();

        paymentMethodToggle.selectedToggleProperty().addListener((obs, oldVal, newVal) -> updateQrSection());
        updateQrSection();
    }

    public void setBookingDTO(BookingDTO bookingDTO) {
        this.bookingDTO = bookingDTO;
        renderBookingSummary();
        updateQrSection();
    }

    @FXML
    private void onPaymentMethodChanged() {
        updateQrSection();
    }

    @FXML
    private void onBack() {
        if (Session.isGuest()) {
            SceneSwitcher.switchContent("/com/busticket/view/guest/GuestInfoView.fxml");
            return;
        }
        SceneSwitcher.switchToBookingSummary();
    }

    @FXML
    private void onConfirmPayment() {
        if (paymentRemote == null) {
            showAlert(Alert.AlertType.ERROR, "Payment Error", "Payment service unavailable.", "Please try again later.");
            return;
        }

        Long bookingId = resolveBookingId();
        if (bookingId == null) {
            showAlert(Alert.AlertType.WARNING, "Booking Missing", "Booking id not found.", "Please restart payment from booking summary.");
            return;
        }

        if (totalAmount <= 0) {
            showAlert(Alert.AlertType.WARNING, "Invalid Amount", "Total amount must be greater than 0.", "Please review selected seats and booking data.");
            return;
        }

        String selectedMethod = resolveSelectedMethod();
        if (selectedMethod == null) {
            showAlert(Alert.AlertType.WARNING, "Payment Method", "Please choose a payment method.", "Select KBZPay, WavePay, or Credit/Debit Card.");
            return;
        }

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setBookingId(bookingId);
        request.setAmount(totalAmount);
        request.setPaymentMethod(toBackendPaymentMethod(selectedMethod));

        if (confirmPaymentButton != null) {
            confirmPaymentButton.setDisable(true);
        }

        try {
            Object result = processPayment(request);
            if (!isPaymentSuccess(result)) {
                showAlert(Alert.AlertType.ERROR, "Payment Failed", "Unable to process payment.", "Please verify payment and try again.");
                return;
            }

            Session.setCurrentBookingContext(bookingId, Session.getCurrentTicketCode(), totalAmount);
            navigateToTicketView();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Payment Failed", "Unable to process payment.", ex.getMessage());
        } finally {
            if (confirmPaymentButton != null) {
                confirmPaymentButton.setDisable(false);
            }
        }
    }

    private void initializePaymentMethods() {
        kbzPayRadio.setUserData(METHOD_KBZPAY);
        wavePayRadio.setUserData(METHOD_WAVEPAY);
        cardRadio.setUserData(METHOD_CARD);
        cardRadio.setSelected(true);
    }

    private void initializeRemotes() {
        try {
            paymentRemote = RMIClient.getPaymentRemote();
        } catch (Exception ex) {
            paymentRemote = null;
            showAlert(Alert.AlertType.ERROR, "Connection Error", "Payment service unavailable.", ex.getMessage());
        }
    }

    private void renderBookingSummary() {
        tripDTO = resolveTripData();
        selectedSeats = resolveSelectedSeats();
        totalAmount = resolveTotalAmount(selectedSeats.size());

        if (tripDTO == null) {
            routeValueLabel.setText("-");
            travelDateValueLabel.setText("-");
            departureTimeValueLabel.setText("-");
        } else {
            String route = safe(tripDTO.getOriginCity()) + " \u2192 " + safe(tripDTO.getDestinationCity());
            routeValueLabel.setText(route);
            travelDateValueLabel.setText(tripDTO.getTravelDate() == null ? "-" : tripDTO.getTravelDate().format(DATE_FMT));
            departureTimeValueLabel.setText(tripDTO.getDepartureTime() == null ? "-" : tripDTO.getDepartureTime().format(TIME_FMT));
        }

        selectedSeatsValueLabel.setText(selectedSeats.isEmpty() ? "-" : String.join(", ", selectedSeats));
        totalPriceValueLabel.setText(String.format(Locale.ENGLISH, "$%.2f", totalAmount));
    }

    private TripDTO resolveTripData() {
        TripDTO sessionTrip = Session.getPendingTrip();
        if (sessionTrip != null) {
            return sessionTrip;
        }

        Long tripId = bookingDTO == null ? null : bookingDTO.getTripId();
        if (tripId == null) {
            return null;
        }

        try {
            TripRemote tripRemote = RMIClient.getTripRemote();
            List<TripDTO> trips = tripRemote.getAllTrips();
            if (trips == null) {
                return null;
            }
            return trips.stream()
                    .filter(Objects::nonNull)
                    .filter(trip -> Objects.equals(trip.getTripId(), tripId))
                    .findFirst()
                    .orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<String> resolveSelectedSeats() {
        List<String> dtoSeats = bookingDTO == null ? null : bookingDTO.getSeatNumbers();
        if (dtoSeats != null && !dtoSeats.isEmpty()) {
            return dtoSeats.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }

        List<String> sessionSeats = Session.getPendingSeatNumbers();
        if (sessionSeats != null && !sessionSeats.isEmpty()) {
            return sessionSeats.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }

        return List.of();
    }

    private double resolveTotalAmount(int seatCount) {
        if (bookingDTO != null && bookingDTO.getTotalPrice() != null && bookingDTO.getTotalPrice() > 0) {
            return bookingDTO.getTotalPrice();
        }

        Double sessionAmount = Session.getCurrentBookingAmount();
        if (sessionAmount != null && sessionAmount > 0) {
            return sessionAmount;
        }

        if (tripDTO == null || seatCount <= 0) {
            return 0.0;
        }
        return tripDTO.getPrice() * seatCount;
    }

    private Long resolveBookingId() {
        if (bookingDTO != null && bookingDTO.getBookingId() != null) {
            return bookingDTO.getBookingId();
        }
        return Session.getCurrentBookingId();
    }

    private String resolveSelectedMethod() {
        if (paymentMethodToggle == null || paymentMethodToggle.getSelectedToggle() == null) {
            return null;
        }
        Object userData = paymentMethodToggle.getSelectedToggle().getUserData();
        return userData == null ? null : userData.toString();
    }

    private void updateQrSection() {
        String selectedMethod = resolveSelectedMethod();
        boolean mobileBanking = METHOD_KBZPAY.equals(selectedMethod) || METHOD_WAVEPAY.equals(selectedMethod);

        qrSection.setVisible(mobileBanking);
        qrSection.setManaged(mobileBanking);

        if (!mobileBanking) {
            qrImageView.setImage(null);
            accountNameValueLabel.setText("-");
            accountPhoneValueLabel.setText("-");
            instructionLabel.setText("Scan to pay");
            return;
        }

        if (METHOD_KBZPAY.equals(selectedMethod)) {
            accountNameValueLabel.setText("Bus Ticket System (KBZPay)");
            accountPhoneValueLabel.setText("09-7711-22334");
        } else {
            accountNameValueLabel.setText("Bus Ticket System (WavePay)");
            accountPhoneValueLabel.setText("09-8822-33445");
        }

        instructionLabel.setText("Scan to pay");

        String payload = buildQrPayload(selectedMethod);
        try {
            qrImageView.setImage(generateQrCodeImage(payload, 220, 220));
        } catch (WriterException ex) {
            qrImageView.setImage(null);
            showAlert(Alert.AlertType.WARNING, "QR Generation Error", "Unable to generate QR code.", ex.getMessage());
        }
    }

    private String buildQrPayload(String selectedMethod) {
        Long bookingId = resolveBookingId();
        String bookingPart = bookingId == null ? "N/A" : bookingId.toString();
        return "bookingId=" + bookingPart
                + "|amount=" + String.format(Locale.ENGLISH, "%.2f", totalAmount)
                + "|method=" + selectedMethod;
    }

    private Object processPayment(PaymentRequestDTO request) throws Exception {
        try {
            Method processPaymentMethod = paymentRemote.getClass().getMethod("processPayment", PaymentRequestDTO.class);
            return processPaymentMethod.invoke(paymentRemote, request);
        } catch (NoSuchMethodException ignored) {
            PaymentDTO fallbackRequest = new PaymentDTO();
            fallbackRequest.setBookingId(request.getBookingId());
            fallbackRequest.setPaidAmount(request.getAmount());
            fallbackRequest.setPaymentMethod(request.getPaymentMethod());
            fallbackRequest.setPaymentStatus(PaymentStatus.PAID.name());
            return paymentRemote.processPayment(fallbackRequest);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getTargetException();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new RuntimeException(cause);
        }
    }

    private boolean isPaymentSuccess(Object response) {
        if (response == null) {
            return false;
        }

        if (response instanceof Boolean result) {
            return result;
        }

        if (response instanceof PaymentDTO paymentResponse) {
            String status = paymentResponse.getPaymentStatus();
            boolean paid = status == null || PaymentStatus.PAID.name().equalsIgnoreCase(status);
            return paymentResponse.getPaymentId() != null && paid;
        }

        return false;
    }

    private String toBackendPaymentMethod(String selectedMethod) {
        if (METHOD_CARD.equals(selectedMethod)) {
            return PaymentMethod.CARD.name();
        }
        return PaymentMethod.MOBILE_BANKING.name();
    }

    private void navigateToTicketView() {
        try {
            SceneSwitcher.switchContent("/com/busticket/view/passenger/TicketView.fxml");
        } catch (RuntimeException ex) {
            SceneSwitcher.switchContent("/com/busticket/view/passenger/TicketSuccessView.fxml");
        }
    }

    private Image generateQrCodeImage(String payload, int width, int height) throws WriterException {
        BitMatrix bitMatrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, width, height);
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return image;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
