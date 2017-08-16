package io.prometheus.wls.rest.domain;

import java.util.regex.Pattern;

public class SnakeCaseUtil {
    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("([a-z0-9])([A-Z])");

    static String convert(String s) {
        return SNAKE_CASE_PATTERN.matcher(s).replaceAll("$1_$2").toLowerCase();
    }

    public static boolean isCompliant(String s) {
        return s.equals(convert(s));
    }
}
