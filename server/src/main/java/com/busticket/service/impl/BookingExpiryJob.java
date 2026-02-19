package com.busticket.service.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class BookingExpiryJob {
    private final BookingServiceImpl bookingService;
    private final ScheduledExecutorService scheduler;
    private final int expiryMinutes;

    public BookingExpiryJob(BookingServiceImpl bookingService, int expiryMinutes) {
        this.bookingService = bookingService;
        this.expiryMinutes = expiryMinutes;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::runOnce, 1, 1, TimeUnit.MINUTES);
    }

    private void runOnce() {
        bookingService.cancelExpiredPending(expiryMinutes);
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "booking-expiry-job");
            t.setDaemon(true);
            return t;
        }
    }
}
