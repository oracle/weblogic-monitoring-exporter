package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import com.sun.management.OperatingSystemMXBean;

import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;

/**
 * @author Russell Gold
 */
class MetricsStream extends PrintStream {
    private static final double NANOSEC_PER_SECONDS = 1000000000;

    private final PerformanceProbe performanceProbe;
    private final long startTime;
    private final long startCpu;

    private int scrapeCount;

    MetricsStream(OutputStream outputStream, PerformanceProbe performanceProbe) {
        super(outputStream);
        this.performanceProbe = performanceProbe;
        startTime = performanceProbe.getCurrentTime();
        startCpu = performanceProbe.getCurrentCpu();
    }

    MetricsStream(OutputStream outputStream) throws IOException {
        this(outputStream, new PlatformPeformanceProbe());
    }

    void printMetric(String name, Object value) {
        println(name + " " + value);
        scrapeCount++;
    }

    void printPerformanceMetrics() {
        printf( "%s %d%n", getCountName(), scrapeCount);
        printf("%s %.2f%n", getDurationName(), toSeconds(getElapsedTime()));
        printf("%s %.2f%n", getCpuUsageName(), toSeconds(getCpuUsed()));
    }

    private String getDurationName() {
        return "wls_scrape_duration_seconds" + LiveConfiguration.getPerformanceQualifier();
    }

    private String getCpuUsageName() {
        return "wls_scrape_cpu_seconds" + LiveConfiguration.getPerformanceQualifier();
    }

    private String getCountName() {
        return "wls_scrape_mbeans_count_total" + LiveConfiguration.getPerformanceQualifier();
    }

    private long getElapsedTime() {
        return performanceProbe.getCurrentTime() - startTime;
    }

    private double toSeconds(long nanoSeconds) {
        return nanoSeconds / NANOSEC_PER_SECONDS;
    }

    private long getCpuUsed() {
        return performanceProbe.getCurrentCpu() - startCpu;
    }

    interface PerformanceProbe {
        long getCurrentTime();
        long getCurrentCpu();
    }

    private static class PlatformPeformanceProbe implements PerformanceProbe {
        private final OperatingSystemMXBean osMBean;

        PlatformPeformanceProbe() throws IOException {
            MBeanServerConnection mbsc = ManagementFactory.getPlatformMBeanServer();
            osMBean = ManagementFactory.newPlatformMXBeanProxy(mbsc,
                                                       ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
                                                       OperatingSystemMXBean.class);
        }

        @Override
        public long getCurrentTime() {
            return System.nanoTime();
        }

        @Override
        public long getCurrentCpu() {
            return osMBean.getProcessCpuTime();
        }
    }
}
