package io.prometheus.wls.rest.domain;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.junit.MatcherAssert.assertThat;

@SuppressWarnings("ThrowableNotThrown")
public class ConfigurationExceptionTest {
    @Test
    public void afterAddMultipleContexts_messageContainsFullContext() throws Exception {
        ConfigurationException exception = new ConfigurationException("Something went wrong");

        exception.addContext("second");
        exception.addContext("first");

        assertThat(exception.getMessage(), equalTo("Something went wrong at first.second"));
    }
}