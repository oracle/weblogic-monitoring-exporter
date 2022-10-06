// Copyright (c) 2017, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.meterware.simplestub.Stub.createStrictStub;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

class ConfigurationUpdaterImplTest {

    private static final long TIMESTAMP_1 = 17;
    private static final String CONFIGURATION_1 = "first yaml config";
    private static final String RESPONSE_1 =
            "{\n" +
               quoted("timestamp") +     ":" + TIMESTAMP_1 + ",\n" +
               quoted("configuration") + ':' + quoted(CONFIGURATION_1) + "\n" +
             "}";

    private static final long TIMESTAMP_2 = 85;
    private static final String CONFIGURATION_2 = "second yaml config";
    private static final String RESPONSE_2 =
            "{\n" +
               quoted("timestamp") +     ":" + TIMESTAMP_2 + ",\n" +
               quoted("configuration") + ':' + quoted(CONFIGURATION_2) + "\n" +
             "}";
    private static final int REFRESH_INTERVAL = 10;

    private final WebClientFactoryStub factory = new WebClientFactoryStub();
    private final ClockStub clock = createStrictStub(ClockStub.class);
    private final ConfigurationUpdaterImpl impl = new ConfigurationUpdaterImpl(clock, factory);
    private final ErrorLog errorLog = new ErrorLog();

    private static String quoted(String aString) {
        return '"' + aString + '"';
    }

    @BeforeEach
    public void setUp() {
        impl.configure("url", REFRESH_INTERVAL);
    }

    @Test
    void whenUnableToReachServer_returnedTimestampIsZero() {
        factory.throwWebClientException(new WebClientException());

        assertThat(impl.getLatestConfigurationTimestamp(), equalTo(0L));
    }

    @Test
    void whenUnableToReachServer_addReasonToLog() {
        factory.throwWebClientException(new WebClientException("Unable to reach server"));
        impl.setErrorLog(errorLog);

        impl.getLatestConfigurationTimestamp();

        assertThat(errorLog.getErrors(), containsString("Unable to reach server"));
    }

    @Test
    void extractTimestampFromReply() {
        factory.addJsonResponse(RESPONSE_1);

        assertThat(impl.getLatestConfigurationTimestamp(), equalTo(TIMESTAMP_1));
    }

    @Test
    void whenUpdateFetched_specifyConfiguredUrl() {
        impl.configure("http://repeater/", 0);

        impl.getLatestConfigurationTimestamp();

        assertThat(factory.getClientUrl(), equalTo("http://repeater/"));
    }

    @Test
    void whenAskedForConfigurationWithinUpdateInterval_returnCachedValue() {
        clock.setCurrentMsec(0);
        factory.addJsonResponse(RESPONSE_1);
        impl.getLatestConfigurationTimestamp();

        clock.incrementSeconds(REFRESH_INTERVAL / 2);
        factory.addJsonResponse(RESPONSE_2);

        assertThat(impl.getLatestConfigurationTimestamp(), equalTo(TIMESTAMP_1));
    }

    @Test
    void whenAskedForConfigurationAfterUpdateInterval_returnNewValue() {
        clock.setCurrentMsec(0);
        factory.addJsonResponse(RESPONSE_1);
        impl.getLatestConfigurationTimestamp();

        clock.incrementSeconds(REFRESH_INTERVAL);
        factory.addJsonResponse(RESPONSE_2);

        assertThat(impl.getLatestConfigurationTimestamp(), equalTo(TIMESTAMP_2));
    }

    @Test
    void afterRetrieveUpdate_returnIt() {
        factory.addJsonResponse(RESPONSE_1);

        assertThat(impl.getUpdate().getConfiguration(), equalTo(CONFIGURATION_1));
    }

    @Test
    void onShareConfiguration_connectToConfiguredUrl() {
        impl.configure("http://posttarget", 0);

        impl.shareConfiguration(CONFIGURATION_1);

        assertThat(factory.getClientUrl(), equalTo("http://posttarget"));
    }

    @Test
    void onShareConfiguration_sendsConfigurationInJsonObject() {
        impl.shareConfiguration(CONFIGURATION_1);

        assertThat(factory.getPostedString(), hasJsonPath("$.configuration", equalTo(CONFIGURATION_1)));
    }

    @Test
    void onShareConfiguration_sendsTimestampInJsonObject() {
        clock.setCurrentMsec(23);

        impl.shareConfiguration(CONFIGURATION_1);

        assertThat(factory.getPostedString(), hasJsonPath("$.timestamp", equalTo(23)));
    }

    @Test
    void whenUnableToShareConfiguration_logProblem() {
        factory.throwWebClientException(new WebClientException("Unable to reach server"));
        impl.setErrorLog(errorLog);

        impl.shareConfiguration(CONFIGURATION_1);

        assertThat(errorLog.getErrors(), containsString("Unable to reach server"));
    }

}
