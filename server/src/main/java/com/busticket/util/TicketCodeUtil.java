package com.busticket.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class TicketCodeUtil {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private TicketCodeUtil() {
    }

    public static String generate() {
        String timePart = TS.format(LocalDateTime.now());
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "BTS-" + timePart + "-" + randomPart;
    }
}
