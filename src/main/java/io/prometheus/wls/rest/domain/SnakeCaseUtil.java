package io.prometheus.wls.rest.domain;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import java.util.regex.Pattern;

/**
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
