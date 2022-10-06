// Copyright (c) 2017, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import com.meterware.simplestub.SystemPropertySupport;
import com.oracle.wls.exporter.matchers.PrometheusMetricsMatcher;
import com.oracle.wls.exporter.webapp.HttpServletRequestStub;
import com.oracle.wls.exporter.webapp.ServletUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.oracle.wls.exporter.MetricsStreamTest.LocaleSupport.setFrenchLocale;
import static com.oracle.wls.exporter.matchers.PrometheusMetricsMatcher.followsPrometheusRules;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;

/**
 * @author Russell Gold
 */
class MetricsStreamTest {
    private static final long NANOSEC_PER_SECONDS = 1000000000;
    private static final String LINE_SEPARATOR = "line.separator";
    private static final String WINDOWS_LINE_SEPARATOR = "\r\n";
    private static final String HOST_NAME = "wlshost";
    private static final int HOST_PORT = 7201;
    private static final String INSTANCE = HOST_NAME + ':' + HOST_PORT;
    private static final SortedMap<String,String> QUALIFICATIONS
          = new TreeMap<>(ImmutableMap.of("instance", INSTANCE));
    private final PerformanceProbeStub performanceProbe = new PerformanceProbeStub();
    private final HttpServletRequestStub postRequest = HttpServletRequestStub.createPostRequest().withHostName(HOST_NAME).withPort(HOST_PORT);
    private ByteArrayOutputStream baos;
    private MetricsStream metrics;
    private final List<Memento> mementos = new ArrayList<>();

    @BeforeEach
    public void setUp() throws NoSuchFieldException {
        initMetricsStream();
        ServletUtils.setServer(postRequest);
        mementos.add(SystemPropertySupport.preserve(LINE_SEPARATOR));
        mementos.add(StaticStubSupport.preserve(System.class, "lineSeparator"));
    }

    private void initMetricsStream() {
        baos = new ByteArrayOutputStream();
        metrics = new MetricsStream(INSTANCE, new PrintStream(baos), performanceProbe);
    }

    @AfterEach
    public void tearDown() {
        mementos.forEach(Memento::revert);
    }

    @Test
    void whenNoMetricsScraped_reportNoneScraped() {
        assertThat(getPrintedMetrics(),
                containsString(getQualifiedPlatformMetricName("wls_scrape_mbeans_count_total") + " 0"));
    }

    private String getPrintedMetrics() {
        metrics.printPlatformMetrics();
        return baos.toString();
    }

    @Test
    void whenMetricsPrinted_eachHasItsOwnLineSeparatedByCarriageReturns() {
        metrics.printMetric("a", 12);
        metrics.printMetric("b", 120);
        metrics.printMetric("c", 0);

        assertThat(getPrintedMetricValues(), hasItems("12", "120", "0", "3"));
    }

    @Test
    void whenMetricsPrintedOnWindows_eachHasItsOwnLineSeparatedByCarriageReturns() throws NoSuchFieldException {
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
        return Arrays.stream(getPrintedMetrics()
              .split(System.lineSeparator()))
              .map(PrometheusMetricsMatcher::getMetricValue)
              .collect(Collectors.toList());
    }

    @Test
    void afterMetricsScraped_reportScrapedCount() {
        metrics.printMetric("a", 12);
        metrics.printMetric("b", 120);
        metrics.printMetric("c", 0);

        assertThat(getPrintedMetrics(),
                containsString(getQualifiedPlatformMetricName("wls_scrape_mbeans_count_total") + " 3"));
    }


    private String getQualifiedPlatformMetricName(String metricName) {
        return metricName + '{' + QUALIFICATIONS.entrySet().stream().map(this::toQualification).collect(Collectors.joining(",")) + '}';
    }

    private String toQualification(Map.Entry<String,String> entry) {
        return String.format("%s=\"%s\"", entry.getKey(), entry.getValue());
    }

    @Test
    void afterTimePasses_reportScrapeDuration() {
        performanceProbe.incrementElapsedTime(12.4);

        assertThat(getPrintedMetrics(),
                containsString(getQualifiedPlatformMetricName("wls_scrape_duration_seconds") + " 12.40"));
    }

    @Test
    void afterProcessing_reportCpuPercent() {
        performanceProbe.incrementCpuTime(3.2);

        assertThat(getPrintedMetrics(),
                containsString(getQualifiedPlatformMetricName("wls_scrape_cpu_seconds") + " 3.20"));
    }

    @Test
    void includeVersionStringInMetrics() {
        metrics.printPlatformMetrics();

        assertThat(baos.toString(), containsString(LiveConfiguration.getVersionString()));
     }

    @Test
    void producedMetricsAreCompliant() {
        performanceProbe.incrementElapsedTime(20);
        performanceProbe.incrementCpuTime(3);

        assertThat(getPrintedMetrics(), followsPrometheusRules());
    }

    @Test
    void alwaysUseUSEncodingForMetrics() {
        mementos.add(setFrenchLocale());

        metrics.printMetric("scraped value", 3.14);

        assertThat(baos.toString(), containsString("."));
    }

    @Test
    void alwaysUseUSEncodingForPlatformMetrics() {
        mementos.add(setFrenchLocale());

        metrics.printPlatformMetrics();

        assertThat(baos.toString(), containsString("."));
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

    static class LocaleSupport implements Memento {
        private final Locale savedLocale = Locale.getDefault();

        private LocaleSupport(Locale locale) {
            Locale.setDefault(locale);
        }

        static Memento setFrenchLocale() {
            return new LocaleSupport(new Locale.Builder().setLanguage("fr").setRegion("FR").build());
        }

        @Override
        public void revert() {
            Locale.setDefault(savedLocale);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Locale getOriginalValue() {
            return savedLocale;
        }
    }
}
