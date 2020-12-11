// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.domain;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.junit.MatcherAssert.assertThat;

/**
 * @author Russell Gold
 */
public class MapUtilsTest {

    private static final String[] STRING_ARRAY = {"1", "2", "3"};

    @Test
    public void whenStringArrayValueIsStringArray_returnAsIs() throws Exception {
        Map<String,Object> map = ImmutableMap.of("values", STRING_ARRAY);

        assertThat(MapUtils.getStringArray(map, "values"), arrayContaining(STRING_ARRAY));
    }

    @Test
    public void whenStringArrayValueIsSingleObject_returnAsLengthOneArray() throws Exception {
        Map<String,Object> map = ImmutableMap.of("values", 33);

        assertThat(MapUtils.getStringArray(map, "values"), arrayContaining("33"));
    }

    @Test
    public void whenStringArrayValueIsList_returnAsArray() throws Exception {
        Map<String,Object> map = ImmutableMap.of("values", Arrays.asList(7, 8, true));

        assertThat(MapUtils.getStringArray(map, "values"), arrayContaining("7", "8", "true"));
    }
}
