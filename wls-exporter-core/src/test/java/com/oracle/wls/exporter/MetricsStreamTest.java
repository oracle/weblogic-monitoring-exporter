// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import com.meterware.simplestub.SystemPropertySupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.oracle.wls.exporter.matchers.PrometheusMetricsMatcher.followsPrometheusRules;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;

/**
 * @author Russell Gold
 */
public class MetricsStreamTest {
    private static final long NANOSEC_PER_SECONDS = 1000000000;
    private static final String LINE_SEPARATOR = "line.separator";
    private static final String WINDOWS_LINE_SEPARATOR = "\r\n";
    private final PerformanceProbeStub performanceProbe = new PerformanceProbeStub();
    private final HttpServletRequestStub postRequest = HttpServletRequestStub.createPostRequest().withHost("wlshost").withPort(7201);
    private ByteArrayOutputStream baos;
    private MetricsStream metrics;
    private final List<Memento> mementos = new ArrayList<>();

    @Before
    public void setUp() throws NoSuchFieldException {
        initMetricsStream();
        LiveConfiguration.setServer(postRequest);
        mementos.add(SystemPropertySupport.preserve(LINE_SEPARATOR));
        mementos.add(StaticStubSupport.preserve(System.class, "lineSeparator"));
    }

    private void initMetricsStream() {
        baos = new ByteArrayOutputStream();
        metrics = new MetricsStream(postRequest, new PrintStream(baos), performanceProbe);
    }

    @After
    public void tearDown() {
        mementos.forEach(Memento::revert);
    }

    @Test
    public void whenNoMetricsScraped_reportNoneScraped() {
        assertThat(getPrintedMetrics(),
                containsString("wls_scrape_mbeans_count_total{instance=\"wlshost:7201\"} 0"));
    }

    private String getPrintedMetrics() {
        metrics.printPerformanceMetrics();
        return baos.toString();
    }

    @Test
    public void whenMetricsPrinted_eachHasItsOwnLineSeparatedByCarriageReturns() {
        metrics.printMetric("a", 12);
        metrics.printMetric("b", 120);
        metrics.printMetric("c", 0);

        assertThat(getPrintedMetricValues(), hasItems("12", "120", "0", "3"));
    }

    @Test
    public void whenMetricsPrintedOnWindows_eachHasItsOwnLineSeparatedByCarriageReturns() throws NoSuchFieldException {
        simulateWindows();

        metrics.printMetric("a", 12);
        metrics.printMetric("b", 120);
        metrics.printMetric("c", 0);

        assertThat(getPrintedMetricValues(), hasItems("12", "120", "0", "3"));
    }

    private void simulateWindows() throws NoSuchFieldException {
        StaticStubSupport.install(System.class, "lineSeparator", WINDOWS_LINE_SEPARATOR);
        System.setProperty(LINE_SEPARATOR, WINDOWS_LINE_SEPARATOR);
        initMetricsStream();
    }

    private List<String> getPrintedMetricValues() {
        return Arrays.stream(getPrintedMetrics().split("\n")).map(l -> l.split(" ")[1]).collect(Collectors.toList());
    }

    @Test
    public void afterMetricsScraped_reportScrapedCount() {
        metrics.printMetric("a", 12);
        metrics.printMetric("b", 120);
        metrics.printMetric("c", 0);

        assertThat(getPrintedMetrics(),
                containsString("wls_scrape_mbeans_count_total{instance=\"wlshost:7201\"} 3"));
    }

    @Test
    public void afterTimePasses_reportScrapeDuration() {
        performanceProbe.incrementElapsedTime(12.4);

        assertThat(getPrintedMetrics(),
                containsString("wls_scrape_duration_seconds{instance=\"wlshost:7201\"} 12.40"));
    }

    @Test
    public void afterProcessing_reportCpuPercent() {
        performanceProbe.incrementCpuTime(3.2);

        assertThat(getPrintedMetrics(),
                containsString("wls_scrape_cpu_seconds{instance=\"wlshost:7201\"} 3.20"));
    }

    @Test
    public void producedMetricsAreCompliant() {
        performanceProbe.incrementElapsedTime(20);
        performanceProbe.incrementCpuTime(3);

        assertThat(getPrintedMetrics(), followsPrometheusRules());
    }

    @SuppressWarnings("SameParameterValue")
    static class PerformanceProbeStub implements MetricsStream.PerformanceProbe {
        private long currentTime = getRandom();
        private long currentCpu = getRandom();

        private static long getRandom() {
            return ((long) (Math.random() * 100 * NANOSEC_PER_SECONDS));
        }
        void incrementElapsedTime(double seconds) {
            currentTime += seconds * NANOSEC_PER_SECONDS;
        }

        void incrementCpuTime(double seconds) {
            currentCpu += seconds * NANOSEC_PER_SECONDS;
        }

        @Override
        public long getCurrentTime() {
            return currentTime;
        }

        @Override
        public long getCurrentCpu() {
            return currentCpu;
        }
    }
}
