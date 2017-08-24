package io.prometheus.wls.rest.domain;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author Russell Gold
 */
public class SnakeCaseUtilTest {

    @Test
    public void convertToSnakeCase() throws Exception {
        assertThat(SnakeCaseUtil.convert("simple"), equalTo("simple"));
        assertThat(SnakeCaseUtil.convert("already_snake_case"), equalTo("already_snake_case"));
        assertThat(SnakeCaseUtil.convert("isCamelCase"), equalTo("is_camel_case"));
        assertThat(SnakeCaseUtil.convert("PascalCase"), equalTo("pascal_case"));
    }

    @Test
    public void verifiesSnakeCase() throws Exception {
        assertThat(SnakeCaseUtil.isCompliant("simple"), is(true));
        assertThat(SnakeCaseUtil.isCompliant("an_example_with_multiple_words"), is(true));
        assertThat(SnakeCaseUtil.isCompliant("camelCaseWithMultipleWords"), is(false));
        assertThat(SnakeCaseUtil.isCompliant("PascalCase"), is(false));
    }
}
