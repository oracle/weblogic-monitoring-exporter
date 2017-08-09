package io.prometheus.wls.rest.matchers;

import org.junit.Test;

import java.util.Arrays;

import static io.prometheus.wls.rest.matchers.PrometheusMetricsMatcher.followsPrometheusRules;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrometheusMetricsMatcherTest {

  private final PrometheusMetricsMatcher matcher = followsPrometheusRules();

  @Test
  public void whenMetricsAreGrouped_matcherPasses() throws Exception {
    assertTrue(matcher.matches(toHtml(COMPLIANT_LIST)));
  }

  private final String[] COMPLIANT_LIST =
        {"metric1 1","metric2{name='red'} 23","metric2{name='blue'} 34"};

  private String toHtml(String... metrics) {
    return Arrays.stream(metrics).map((s) -> s.replace('\'', '"')).collect(joining("\n"));
  }

  @Test
  public void whenMetricsAreInterspersed_matcherFails() throws Exception {
    assertFalse(matcher.matches(toHtml(MISORDERED_LIST)));
  }

  private final String[] MISORDERED_LIST =
        {"metric2{name='red'} 23","metric1 1","metric2{name='blue'} 34"};
}
