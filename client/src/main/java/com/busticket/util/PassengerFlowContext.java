package com.busticket.util;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.TripDTO;

public final class PassengerFlowContext {
    private static TripDTO selectedTrip;
    private static BookingDTO currentBooking;

    private PassengerFlowContext() {
    }

    public static TripDTO getSelectedTrip() {
        return selectedTrip;
    }

    public static void setSelectedTrip(TripDTO selectedTrip) {
        PassengerFlowContext.selectedTrip = selectedTrip;
    }

    public static BookingDTO getCurrentBooking() {
        return currentBooking;
    }

    public static void setCurrentBooking(BookingDTO currentBooking) {
        PassengerFlowContext.currentBooking = currentBooking;
    }
}
