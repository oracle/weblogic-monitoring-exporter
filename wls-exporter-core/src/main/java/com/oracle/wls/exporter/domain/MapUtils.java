// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.util.List;
import java.util.Map;

/**
 * Some common utilities for dealing with the maps produced for json and yaml
 *
 * @author Russell Gold
 */
public class MapUtils {

    private static final String ILLEGAL_VALUE_FORMAT = "Illegal value for %s: %s. Value must be %s";

    /**
     * Attempts to retrieve the specified value as an integer. It can recognize the value
     * either as a Number object or a string to be parsed.
     * @param map a map containing the value
     * @param key the map key at which the value is found
     * @return an integer value derived from the item in the map
     */
    static Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number)
            return ((Number) value).intValue();

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            String expectedType = "an integer";
            throw createBadTypeException(key, value, expectedType);
        }
    }

    /**
     * Attempts to retrieve the specified value as a boolean. It can recognize the value
     * either as a Boolean object or a string.
     * @param map a map containing the value
     * @param key the map key at which the value is found
     * @return a boolean value derived from the item in the map
     */
    static Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean)
            return (Boolean) value;

        if (inValues(value, TRUE_VALUES)) return true;
        if (inValues(value, FALSE_VALUES)) return false;
        throw new ConfigurationException("Unable to interpret '" + value + "' as a boolean value");
    }

    private final static String[] TRUE_VALUES = {"true", "t", "yes", "on", "y"};
    private final static String[] FALSE_VALUES = {"false", "f", "no", "off", "n"};

    private static boolean inValues(Object candidate, String... matches) {
        for (String match : matches)
            if (candidate.toString().equalsIgnoreCase(match)) return true;

        return false;
    }

    /**
     * Creates an exception which describes the failure to interpret a map value
     * @param key the map key used to retrieve the value
     * @param value the actual problematic value found
     * @param expectedType a description of the type permitted
     * @return an exception which can be thrown to report a problem
     */
    static IllegalArgumentException createBadTypeException(String key, Object value, String expectedType) {
        return new IllegalArgumentException(String.format(ILLEGAL_VALUE_FORMAT, key, value, expectedType));
    }

    /**
     * Returns the specified map value as a string.
     * @param map a map containing the value
     * @param key the map key at which the value is found
     * @return the string representation of the value
     */
    static String getStringValue(Map<String, Object> map, String key) {
        return map.get(key).toString();
    }

    /**
     * Returns the specified map value as an array of strings. If it is a scalar, it will be
     * returned as an array containing only the found value.
     * @param map a map containing the value
     * @param key the map key at which the value is found
     * @return the string representation of the value
     */
    @SuppressWarnings("unchecked")
    static String[] getStringArray(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List)
            return toStringArray((List<Object>) value);
        else if (!value.getClass().isArray())
            return new String[]{value.toString()};
        else if (value.getClass().getComponentType() == String.class)
            return (String[]) value;
        else
            throw createBadTypeException(key, value, "an array of strings");
    }

    private static String[] toStringArray(List<Object> list) {
        String[] result = new String[list.size()];
        for (int i = 0; i < result.length; i++)
            result[i] = list.get(i).toString();
        return result;
    }

    /**
     * Returns true if the specified value is a null or empty string.
     * @param s a string to check
     * @return true if the string has no contents
     */
    public static boolean isNullOrEmptyString(String s) {
        return s == null || s.length() == 0;
    }

}
