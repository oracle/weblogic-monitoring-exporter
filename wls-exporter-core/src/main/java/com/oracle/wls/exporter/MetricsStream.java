// Copyright (c) 2017, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import javax.management.MBeanServerConnection;

import com.sun.management.OperatingSystemMXBean;

/**
 * A PrintStream that computes metrics for the performance of the exporter itself. It does so by tracking the
 * time from its creation until it is instructed to print those metrics.
 *
 * @author Russell Gold
 */
class MetricsStream extends PrintStream {
    private static final String PROMETHEUS_LINE_SEPARATOR = "\n"; // This is not dependent on the platform running the exporter.
    private static final double NANOSEC_PER_SECONDS = 1000000000;
    static final String CPU_USAGE_METRIC = "wls_scrape_cpu_seconds";
    static final String NUM_MBEANS_SCRAPED_METRIC = "wls_scrape_mbeans_count_total";
    static final String SCRAPE_DURATION_SECONDS_METRIC = "wls_scrape_duration_seconds";
    static final String FREE_MEMORY_METRIC = "exporter_free_memory_bytes";
    static final String MESSAGES_DIAGNOSTIC_SIZE = "exporter_messages_diagnostics_bytes";
    static final String MAXIMUM_EXCHANGE_SIZE = "exporter_messages_maximum_exchange_bytes";
    static final String RECENT_MESSAGES_DIAGNOSTIC_SIZE = "exporter_messages_recent_bytes";
    static final String RECENT_RETRIES = "exporter_recent_retries";

    private final PerformanceProbe performanceProbe;
    private final long startTime;
    private final long startCpu;
    private final String instance;

    private int scrapeCount;

    /**
     * Constructs a metrics stream object, installing a performance probe that access system data.
     * @param instance the instance from which metrics are being collected
     * @param outputStream the parent output stream
     * @throws IOException if some error occurs while creating the performance probe
     */
    MetricsStream(String instance, OutputStream outputStream) throws IOException {
        this(instance, outputStream, new PlatformPerformanceProbe());
    }

    /**
     * A constructor for unit testing, allowing the specification of a test version of the performance probe.
     * @param instance the instance from which metrics are being collected
     * @param outputStream the parent output stream
     * @param performanceProbe an object which can return performance data
     */
    MetricsStream(String instance, OutputStream outputStream, PerformanceProbe performanceProbe) {
        super(outputStream);
        this.instance = instance;
        this.performanceProbe = performanceProbe;
        startTime = performanceProbe.getCurrentTime();
        startCpu = performanceProbe.getCurrentCpu();
    }

    /**
     * Prints a single metric, while adding to the count of metrics produced
     * @param name the metric name
     * @param value the metric value
     */
    void printMetric(String name, Object value) {
        print(name + " " + value + PROMETHEUS_LINE_SEPARATOR);
        scrapeCount++;
    }

    /**
     * Prints the summary performance metrics
     */
    void printPlatformMetrics() {
        printPlatformMetric(NUM_MBEANS_SCRAPED_METRIC, scrapeCount);
        printPlatformMetric(SCRAPE_DURATION_SECONDS_METRIC, toSecondsString(getElapsedTime()));
        printPlatformMetric(CPU_USAGE_METRIC, toSecondsString(getCpuUsed()));
        printPlatformMetric(FREE_MEMORY_METRIC, Runtime.getRuntime().freeMemory());
        printPlatformMetric(MESSAGES_DIAGNOSTIC_SIZE, WlsRestExchanges.getMessageAllocation());
        printPlatformMetric(MAXIMUM_EXCHANGE_SIZE, WlsRestExchanges.getMaximumExchangeLength());
        printPlatformMetric(RECENT_MESSAGES_DIAGNOSTIC_SIZE, WlsRestExchanges.getTotalExchangeLengthOverPastTenMinutes());
        printPlatformMetric(RECENT_RETRIES, AuthenticatedCall.getRecentRetries());
        printMetric(getExporterVersionName(), 1);
    }

    private void printPlatformMetric(String metricName, Object value) {
        printMetric(metricName + getPlatformQualifier(), value);
    }

    /**
     * Returns the qualifiers to add to the platform metrics, specifying the configured server
     * @return a metrics qualifier string
     */
    private String getPlatformQualifier() {
        return String.format("{instance=\"%s\"}", instance);
    }

    private String getExporterVersionName() {
        return "exporter_version" + getExporterVersionQualifier();
    }

    /**
     * Returns the qualifiers to add to the version metric.
     */
    private String getExporterVersionQualifier() {
        return String.format("{instance=\"%s\",version=\"%s\"}", instance, LiveConfiguration.getVersionString());
    }

    private long getElapsedTime() {
        return performanceProbe.getCurrentTime() - startTime;
    }

    private String toSecondsString(long nanoSeconds) {
        return String.format(Locale.US, "%.2f", nanoSeconds / NANOSEC_PER_SECONDS);
    }

    private long getCpuUsed() {
        return performanceProbe.getCurrentCpu() - startCpu;
    }

    interface PerformanceProbe {
        long getCurrentTime();
        long getCurrentCpu();
    }

    private static class PlatformPerformanceProbe implements PerformanceProbe {
        private final OperatingSystemMXBean osMBean;

        PlatformPerformanceProbe() throws IOException {
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
