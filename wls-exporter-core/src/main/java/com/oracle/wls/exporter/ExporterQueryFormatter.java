package com.oracle.wls.exporter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ExporterQueryFormatter {
    private static final DateTimeFormatter formatter
            = DateTimeFormatter.ofPattern("kk:mm:ss.SSS zzz").withZone(ZoneId.of("UTC"));

    static String format(String remoteHost, Instant instant) {
        return "Received from " + remoteHost + " at " + formatter.format(instant);
    }
}
