package io.prometheus.wls.rest.matchers;

import java.util.function.Predicate;

class MetricsUtils {
    static String getMetricName(String s) {
        String spec = s.trim().split(" ")[0];
        return spec.contains("{") ? spec.split("\\{")[0] : spec;
    }

    static class Uniq implements Predicate<String> {
        String last = "";
        @Override
        public boolean test(String s) {
            try {
                return s != null && !s.equals(last);
            } finally {
                last = s;
            }
        }
    }
}
