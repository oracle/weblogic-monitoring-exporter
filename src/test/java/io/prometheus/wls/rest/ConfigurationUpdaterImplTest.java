package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;

import static com.meterware.simplestub.Stub.createStrictStub;
import static io.prometheus.wls.rest.domain.JsonPathMatcher.hasJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ConfigurationUpdaterImplTest {

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

    private WebClientFactoryStub factory = createStrictStub(WebClientFactoryStub.class);
    private ClockStub clock = createStrictStub(ClockStub.class);
    private ConfigurationUpdaterImpl impl = new ConfigurationUpdaterImpl(clock, factory);

    private static String quoted(String aString) {
        return '"' + aString + '"';
    }

    @Before
    public void setUp() throws Exception {
        impl.configure("url", REFRESH_INTERVAL);
    }

    @Test
    public void whenUnableToReachServer_returnedTimestampIsZero() throws Exception {
        factory.setException(new WebClientException());

        assertThat(impl.getLatestConfigurationTimestamp(), equalTo(0L));
    }

    @Test
    public void extractTimestampFromReply() throws Exception {
        factory.setResponse(RESPONSE_1);

        assertThat(impl.getLatestConfigurationTimestamp(), equalTo(TIMESTAMP_1));
    }

    @Test
    public void whenUpdateFetched_specifyConfiguredUrl() throws Exception {
        impl.configure("http://repeater/", 0);

        impl.getLatestConfigurationTimestamp();

        assertThat(factory.getClientURL(), equalTo("http://repeater/"));
    }

    @Test
    public void whenAskedForConfigurationWithinUpdateInterval_returnCachedValue() throws Exception {
        clock.setCurrentMsec(0);
        factory.setResponse(RESPONSE_1);
        impl.getLatestConfigurationTimestamp();

        clock.incrementSeconds(REFRESH_INTERVAL / 2);
        factory.setResponse(RESPONSE_2);

        assertThat(impl.getLatestConfigurationTimestamp(), equalTo(TIMESTAMP_1));
    }

    @Test
    public void whenAskedForConfigurationAfterUpdateInterval_returnNewValue() throws Exception {
        clock.setCurrentMsec(0);
        factory.setResponse(RESPONSE_1);
        impl.getLatestConfigurationTimestamp();

        clock.incrementSeconds(REFRESH_INTERVAL);
        factory.setResponse(RESPONSE_2);

        assertThat(impl.getLatestConfigurationTimestamp(), equalTo(TIMESTAMP_2));
    }

    @Test
    public void afterRetrieveUpdate_returnIt() throws Exception {
        factory.setResponse(RESPONSE_1);

        assertThat(impl.getUpdate().getConfiguration(), equalTo(CONFIGURATION_1));
    }

    @Test
    public void onShareConfiguration_connectToConfiguredUrl() throws Exception {
        impl.configure("http://posttarget", 0);

        impl.shareConfiguration(CONFIGURATION_1);

        assertThat(factory.getClientURL(), equalTo("http://posttarget"));
    }

    @Test
    public void onShareConfiguration_sendsConfigurationInJsonObject() throws Exception {
        impl.shareConfiguration(CONFIGURATION_1);

        assertThat(factory.getPostedString(), hasJsonPath("$.configuration").withValue(CONFIGURATION_1));
    }

    @Test
    public void onShareConfiguration_sendsTimestampInJsonObject() throws Exception {
        clock.setCurrentMsec(23);

        impl.shareConfiguration(CONFIGURATION_1);

        assertThat(factory.getPostedString(), hasJsonPath("$.timestamp").withValue(23));
    }

    static abstract class ClockStub extends Clock {
        private long currentMsec;

        void setCurrentMsec(long currentMsec) {
            this.currentMsec = currentMsec;
        }

        void incrementSeconds(long seconds) {
            this.currentMsec += 1000 * seconds;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(currentMsec);
        }
    }
}
