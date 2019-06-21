package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import com.meterware.simplestub.SystemPropertySupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.prometheus.wls.rest.matchers.PrometheusMetricsMatcher.followsPrometheusRules;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Russell Gold
 */
public class MetricsStreamTest {
    private static final long NANOSEC_PER_SECONDS = 1000000000;
    private static final String LINE_SEPARATOR = "line.separator";
    private static final String WINDOWS_LINE_SEPARATOR = "\r\n";
    private PerformanceProbeStub performanceProbe = new PerformanceProbeStub();
    private ByteArrayOutputStream baos;
    private MetricsStream metrics;
    private List<Memento> mementos = new ArrayList<>();

    @Before
    public void setUp() throws NoSuchFieldException {
        initMetricsStream();
        LiveConfiguration.setServer("localhost", 7001);
        mementos.add(SystemPropertySupport.preserve(LINE_SEPARATOR));
        mementos.add(StaticStubSupport.preserve(System.class, "lineSeparator"));
    }

    private void initMetricsStream() {
        baos = new ByteArrayOutputStream();
        metrics = new MetricsStream(new PrintStream(baos), performanceProbe);
    }

    @After
    public void tearDown() {
        mementos.forEach(Memento::revert);
    }

    @Test
    public void whenNoMetricsScraped_reportNoneScraped() {
        assertThat(getPrintedMetrics(),
                containsString("wls_scrape_mbeans_count_total{instance=\"localhost:7001\"} 0"));
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
                containsString("wls_scrape_mbeans_count_total{instance=\"localhost:7001\"} 3"));
    }

    @Test
    public void afterTimePasses_reportScrapeDuration() {
        performanceProbe.incrementElapsedTime(12.4);

        assertThat(getPrintedMetrics(),
                containsString("wls_scrape_duration_seconds{instance=\"localhost:7001\"} 12.40"));
    }

    @Test
    public void afterProcessing_reportCpuPercent() {
        performanceProbe.incrementCpuTime(3.2);

        assertThat(getPrintedMetrics(),
                containsString("wls_scrape_cpu_seconds{instance=\"localhost:7001\"} 3.20"));
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
