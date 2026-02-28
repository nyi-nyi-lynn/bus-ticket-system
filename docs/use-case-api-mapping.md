## Use Case to API Mapping

### Customer
- `Search Trips` -> `BusTicketRemote.searchTrips(originCity, destinationCity, travelDate)`
- `Select Seat` -> `BusTicketRemote.getAvailableSeats(tripId)`
- `Make Payment` -> `BusTicketRemote.makePayment(bookingId, paymentMethod, paidAmount)`
- `View/Download Ticket` -> `BusTicketRemote.viewTicket(ticketCode)`

### Admin
- `Manage Bus/Route` -> `BusTicketRemote.addBus(bus)`, `BusTicketRemote.addRoute(route)`
- `Manage Schedule` -> `BusTicketRemote.createTrip(trip)`
- `View Sales Report` -> `BusTicketRemote.getSalesReport(fromDate, toDate)`

## Notes
- Ticket QR is represented as payload string (`TICKET:<ticket_code>`) in `TicketDTO`.
- DB schema is in `server/src/main/resources/database/schema.sql`.
