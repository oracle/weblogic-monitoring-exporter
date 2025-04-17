// Copyright (c) 2017, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.ByteArrayInputStream;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import com.oracle.wls.exporter.domain.ExporterConfig;
import com.oracle.wls.exporter.javax.HttpServletRequestStub;
import com.oracle.wls.exporter.javax.ServletUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static com.oracle.wls.exporter.InMemoryResources.withNoParams;
import static com.oracle.wls.exporter.javax.HttpServletRequestStub.HOST_NAME;
import static com.oracle.wls.exporter.javax.HttpServletRequestStub.PORT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * @author Russell Gold
 */
class LiveConfigurationTest {
    private static final String CONFIGURATION =
            "hostName: " + HOST_NAME + "\n" +
            "port: " + PORT + "\n" +
            "queries:\n" + "" +
            "- groups:\n" +
            "    prefix: new_\n" +
            "    key: name\n" +
            "    values: [sample1, sample2]\n";

    private static final String ADDED_CONFIGURATION =
            "hostName: " + HOST_NAME + "\n" +
            "port: " + PORT + "\n" +
            "queries:\n" + "" +
            "- people:\n" +
            "    key: name\n" +
            "    values: [age, sex]\n";

    private static final String COMBINED_CONFIGURATION =
            "hostName: " + HOST_NAME + "\n" +
            "port: " + PORT + "\n" +
            "queries:\n" + "" +
            "- groups:\n" +
            "    prefix: new_\n" +
            "    key: name\n" +
            "    values: [sample1, sample2]\n" +
            "- people:\n" +
            "    key: name\n" +
            "    values: [age, sex]\n";

    private static final String SYNC_URL = "http://coordinator:8999/";
    private static final long REFRESH_INTERVAL = 20L;
    private static final String CONFIGURATION_WITH_SYNC =
            "query_sync:\n" +
            "  url: " + SYNC_URL + "\n" +
            "  interval: " + REFRESH_INTERVAL + "\n" +
            "queries:\n" + "" +
            "- groups:\n" +
            "    prefix: new_\n" +
            "    key: name\n" +
            "    values: [sample1, sample2]\n";

    @BeforeEach
    void setUp() throws Exception {
        InMemoryResources.install();
        ConfigurationUpdaterStub.install();
        ServletUtils.setServer(HttpServletRequestStub.createPostRequest());
    }

    @AfterEach
    void tearDown() {
        InMemoryResources.uninstall();
        ConfigurationUpdaterStub.uninstall();
    }

    @Test
    void afterInitCalled_haveQueries() {
        init(CONFIGURATION);

        assertThat(LiveConfiguration.hasQueries(), is(true));
    }

    private void init(String configuration) {
        InMemoryResources.defineResource(ServletUtils.CONFIG_YML, configuration);
        ServletUtils.initializeConfiguration(withNoParams());
    }

    @Test
    void whenInitNotCalled_haveNoQueries() {
        assertThat(LiveConfiguration.hasQueries(), is(false));
    }

    @Test
    void whenInitCalledWithNoConfig_haveNoQueries() {
        ServletUtils.initializeConfiguration(withNoParams());
        
        assertThat(LiveConfiguration.hasQueries(), is(false));
    }

    @Test
    void afterInitCalled_haveExpectedConfiguration() {
        init(CONFIGURATION);

        assertThat(LiveConfiguration.asString(), equalTo(CONFIGURATION));
    }

    @Test
    void afterInitCalledTwice_haveFirstConfiguration() {
        init(CONFIGURATION);
        init(ADDED_CONFIGURATION);

        assertThat(LiveConfiguration.asString(), equalTo(CONFIGURATION));
    }

    @Test
    void afterInitTimestampIsZero() {
        init(CONFIGURATION);
        
        assertThat(LiveConfiguration.getTimestamp(), equalTo(0L));
    }

    @Test
    void afterReplaceQueryCalled_configurationIsUpdated() {
        init(CONFIGURATION);

        LiveConfiguration.replaceConfiguration(toConfiguration(ADDED_CONFIGURATION));

        assertThat(LiveConfiguration.asString(), equalTo(ADDED_CONFIGURATION));
    }

    @SuppressWarnings("SameParameterValue")
    private ExporterConfig toConfiguration(String configuration) {
        return ExporterConfig.loadConfig(new ByteArrayInputStream(configuration.getBytes()));
    }

    @Test
    void afterReplaceQueryCalled_shareUpdatedConfiguration() {
        init(CONFIGURATION);

        LiveConfiguration.replaceConfiguration(toConfiguration(ADDED_CONFIGURATION));

        assertThat(ConfigurationUpdaterStub.getSharedConfiguration(), equalTo(ADDED_CONFIGURATION));
    }

    @Test
    void afterReplaceQueryCalled_timestampIsUpdated() {
        init(CONFIGURATION);
        long originalTimestamp = LiveConfiguration.getTimestamp();

        LiveConfiguration.replaceConfiguration(toConfiguration(ADDED_CONFIGURATION));

        assertThat(LiveConfiguration.getTimestamp(), greaterThan(originalTimestamp));
    }

    @Test
    void afterAppendQueryCalled_configurationIsUpdated() {
        init(CONFIGURATION);

        LiveConfiguration.appendConfiguration(toConfiguration(ADDED_CONFIGURATION));

        assertThat(LiveConfiguration.asString(), equalTo(COMBINED_CONFIGURATION));
    }

    @Test
    void afterAppendQueryCalled_shareUpdatedConfiguration() {
        init(CONFIGURATION);

        LiveConfiguration.appendConfiguration(toConfiguration(ADDED_CONFIGURATION));

        assertThat(ConfigurationUpdaterStub.getSharedConfiguration(), equalTo(COMBINED_CONFIGURATION));
    }

    @Test
    void afterAppendQueryCalled_timestampIsUpdated() {
        init(CONFIGURATION);
        long originalTimestamp = LiveConfiguration.getTimestamp();

        LiveConfiguration.appendConfiguration(toConfiguration(ADDED_CONFIGURATION));

        assertThat(LiveConfiguration.getTimestamp(), greaterThan(originalTimestamp));
    }

    @Test
    void whenSharedTimestampIndicatesNewConfiguration_updateLiveConfiguration() {
        init(CONFIGURATION);

        long newTimestamp = LiveConfiguration.getTimestamp() + 1;
        ConfigurationUpdaterStub.newConfiguration(newTimestamp, ADDED_CONFIGURATION);

        LiveConfiguration.updateConfiguration();

        assertThat(LiveConfiguration.asString(), equalTo(ADDED_CONFIGURATION));
        assertThat(LiveConfiguration.getTimestamp(), equalTo(newTimestamp));
    }

    @Test
    void whenSharedTimestampIndicatesHaveLatestConfiguration_dontUpdateLiveConfiguration() {
        init(CONFIGURATION);

        long newTimestamp = LiveConfiguration.getTimestamp();
        ConfigurationUpdaterStub.newConfiguration(newTimestamp, ADDED_CONFIGURATION);

        LiveConfiguration.updateConfiguration();

        assertThat(LiveConfiguration.asString(), equalTo(CONFIGURATION));
    }

    @Test
    void whenConfigurationSpecifiesSynchronization_installHttpBasedUpdater() throws Exception {
        init(CONFIGURATION_WITH_SYNC);

        assertThat(getConfigurationUpdater(), instanceOf(ConfigurationUpdaterImpl.class));
    }

    private ConfigurationUpdater getConfigurationUpdater() throws NoSuchFieldException {
        Memento memento = StaticStubSupport.preserve(LiveConfiguration.class, "updater");
        return memento.getOriginalValue();
    }

    @Test
    void whenConfigurationSpecifiesSynchronization_configureUpdater() throws Exception {
        init(CONFIGURATION_WITH_SYNC);

        ConfigurationUpdaterImpl configurationUpdater = (ConfigurationUpdaterImpl) getConfigurationUpdater();
        assertThat(configurationUpdater.getRepeaterUrl(), equalTo(SYNC_URL));
        assertThat(configurationUpdater.getRefreshInterval(), equalTo(REFRESH_INTERVAL));
    }
}
