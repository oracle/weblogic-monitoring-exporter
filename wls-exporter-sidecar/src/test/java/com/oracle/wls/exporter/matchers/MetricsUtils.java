// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.matchers;

import java.util.function.Predicate;

class MetricsUtils {
    static String getMetricName(String s) {
        String spec = s.trim().split(" ")[0];
        return spec.contains("{") ? spec.split("\\{")[0] : spec;
    }

/**
 * @author Russell Gold
 */
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
