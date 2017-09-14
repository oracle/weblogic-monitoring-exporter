package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import io.prometheus.wls.rest.domain.ExporterConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static io.prometheus.wls.rest.InMemoryFileSystem.withNoParams;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

/**
 * @author Russell Gold
 */
public class LiveConfigurationTest {
    private static final String CONFIGURATION =
            "host: localhost\n" +
            "port: 7001\n" +
            "queries:\n" + "" +
            "- groups:\n" +
            "    prefix: new_\n" +
            "    key: name\n" +
            "    values: [sample1, sample2]\n";

    private static final String ADDED_CONFIGURATION = 
            "host: localhost\n" +
            "port: 7001\n" +
            "queries:\n" + "" +
            "- people:\n" +
            "    key: name\n" +
            "    values: [age, sex]\n";

    private static final String COMBINED_CONFIGURATION =
            "host: localhost\n" +
            "port: 7001\n" +
            "queries:\n" + "" +
            "- groups:\n" +
            "    prefix: new_\n" +
            "    key: name\n" +
            "    values: [sample1, sample2]\n" +
            "- people:\n" +
            "    key: name\n" +
            "    values: [age, sex]\n";

    @Before
    public void setUp() throws Exception {
        InMemoryFileSystem.install();
        ConfigurationUpdaterStub.install();
        LiveConfiguration.setServer("localhost", 7001);
    }

    @After
    public void tearDown() throws Exception {
        InMemoryFileSystem.uninstall();
        ConfigurationUpdaterStub.uninstall();
    }

    @Test
    public void afterInitCalled_haveQueries() throws Exception {
        init(CONFIGURATION);

        assertThat(LiveConfiguration.hasQueries(), is(true));
    }

    private void init(String configuration) {
        InMemoryFileSystem.defineResource(LiveConfiguration.CONFIG_YML, configuration);
        LiveConfiguration.init(withNoParams());
    }

    @Test
    public void whenInitNotCalled_haveNoQueries() throws Exception {
        assertThat(LiveConfiguration.hasQueries(), is(false));
    }

    @Test
    public void afterInitCalled_haveExpectedConfiguration() throws Exception {
        init(CONFIGURATION);

        assertThat(LiveConfiguration.asString(), equalTo(CONFIGURATION));
    }

    @Test
    public void afterInitCalledTwice_haveFirstConfiguration() throws Exception {
        init(CONFIGURATION);
        init(ADDED_CONFIGURATION);

        assertThat(LiveConfiguration.asString(), equalTo(CONFIGURATION));
    }

    @Test
    public void afterInitTimestampIsZero() throws Exception {
        init(CONFIGURATION);
        
        assertThat(LiveConfiguration.getTimestamp(), equalTo(0L));
    }

    @Test
    public void afterReplaceQueryCalled_configurationIsUpdated() throws Exception {
        init(CONFIGURATION);

        LiveConfiguration.replaceConfiguration(toConfiguration(ADDED_CONFIGURATION));

        assertThat(LiveConfiguration.asString(), equalTo(ADDED_CONFIGURATION));
    }

    private ExporterConfig toConfiguration(String configuration) {
        return ExporterConfig.loadConfig(new ByteArrayInputStream(configuration.getBytes()));
    }

    @Test
    public void afterReplaceQueryCalled_shareUpdatedConfiguration() throws Exception {
        init(CONFIGURATION);

        LiveConfiguration.replaceConfiguration(toConfiguration(ADDED_CONFIGURATION));

        assertThat(ConfigurationUpdaterStub.getSharedConfiguration(), equalTo(ADDED_CONFIGURATION));
    }

    @Test
    public void afterReplaceQueryCalled_timestampIsUpdated() throws Exception {
        init(CONFIGURATION);
        long originalTimestamp = LiveConfiguration.getTimestamp();

        LiveConfiguration.replaceConfiguration(toConfiguration(ADDED_CONFIGURATION));

        assertThat(LiveConfiguration.getTimestamp(), greaterThan(originalTimestamp));
    }

    @Test
    public void afterAppendQueryCalled_configurationIsUpdated() throws Exception {
        init(CONFIGURATION);

        LiveConfiguration.appendConfiguration(toConfiguration(ADDED_CONFIGURATION));

        assertThat(LiveConfiguration.asString(), equalTo(COMBINED_CONFIGURATION));
    }

    @Test
    public void afterAppendQueryCalled_shareUpdatedConfiguration() throws Exception {
        init(CONFIGURATION);

        LiveConfiguration.appendConfiguration(toConfiguration(ADDED_CONFIGURATION));

        assertThat(ConfigurationUpdaterStub.getSharedConfiguration(), equalTo(COMBINED_CONFIGURATION));
    }

    @Test
    public void afterAppendQueryCalled_timestampIsUpdated() throws Exception {
        init(CONFIGURATION);
        long originalTimestamp = LiveConfiguration.getTimestamp();

        LiveConfiguration.appendConfiguration(toConfiguration(ADDED_CONFIGURATION));

        assertThat(LiveConfiguration.getTimestamp(), greaterThan(originalTimestamp));
    }

    @Test
    public void whenSharedTimestampIndicatesNewConfiguration_updateLiveConfiguration() throws Exception {
        init(CONFIGURATION);

        long newTimestamp = LiveConfiguration.getTimestamp() + 1;
        ConfigurationUpdaterStub.newConfiguration(newTimestamp, ADDED_CONFIGURATION);

        LiveConfiguration.updateConfiguration();

        assertThat(LiveConfiguration.asString(), equalTo(ADDED_CONFIGURATION));
        assertThat(LiveConfiguration.getTimestamp(), equalTo(newTimestamp));
    }

    @Test
    public void whenSharedTimestampIndicatesHaveLatestConfiguration_dontUpdateLiveConfiguration() throws Exception {
        init(CONFIGURATION);

        long newTimestamp = LiveConfiguration.getTimestamp();
        ConfigurationUpdaterStub.newConfiguration(newTimestamp, ADDED_CONFIGURATION);

        LiveConfiguration.updateConfiguration();

        assertThat(LiveConfiguration.asString(), equalTo(CONFIGURATION));
    }

}
