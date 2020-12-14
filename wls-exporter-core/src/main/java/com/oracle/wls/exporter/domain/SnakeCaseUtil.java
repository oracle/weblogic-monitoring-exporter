// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.util.regex.Pattern;

/**
 * A class which can convert strings from camel case to snake case.
 *
 * @author Russell Gold
 */
public class SnakeCaseUtil {
    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("([a-z0-9])([A-Z])");

    static String convert(String s) {
        return SNAKE_CASE_PATTERN.matcher(s).replaceAll("$1_$2").toLowerCase();
    }

    public static boolean isCompliant(String s) {
        return s.equals(convert(s));
    }
}
