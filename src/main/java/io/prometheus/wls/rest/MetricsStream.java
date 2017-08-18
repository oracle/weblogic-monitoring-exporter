package io.prometheus.wls.rest;

import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

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
        if (getElapsedTime() > 0)
            printf("%s %.2f%n", getCpuUsageName(), asPercent(getCpuUsed() / getElapsedTime()));
    }

    private String getDurationName() {
        return "wls_scrape_duration_seconds" + LiveConfiguration.getPerformanceQualifier();
    }

    private String getCpuUsageName() {
        return "wls_scrape_cpu_percent" + LiveConfiguration.getPerformanceQualifier();
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

    private double getCpuUsed() {
        return performanceProbe.getCurrentCpu() - startCpu;
    }

    private double asPercent(double ratio) {
        return 100.0 * ratio;
    }

    interface PerformanceProbe {
        long getCurrentTime();
        long getCurrentCpu();
    }

    private static class PlatformPeformanceProbe implements PerformanceProbe {
        private final ThreadMXBean threadMXBean;
        private long threadId = Thread.currentThread().getId();

        PlatformPeformanceProbe() throws IOException {
            MBeanServerConnection mbsc = ManagementFactory.getPlatformMBeanServer();
            threadMXBean = ManagementFactory.newPlatformMXBeanProxy(mbsc,
                                                       ManagementFactory.THREAD_MXBEAN_NAME,
                                                       ThreadMXBean.class);
        }

        @Override
        public long getCurrentTime() {
            return System.nanoTime();
        }

        @Override
        public long getCurrentCpu() {
            return threadMXBean.getThreadCpuTime(threadId);
        }
    }
}
