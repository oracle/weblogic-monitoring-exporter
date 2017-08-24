package io.prometheus.wls.rest.domain;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

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
