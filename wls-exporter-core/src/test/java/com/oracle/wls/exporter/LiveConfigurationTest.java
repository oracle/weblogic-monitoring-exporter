// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.ByteArrayInputStream;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import com.oracle.wls.exporter.domain.ExporterConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.oracle.wls.exporter.HttpServletRequestStub.HOST;
import static com.oracle.wls.exporter.HttpServletRequestStub.PORT;
import static com.oracle.wls.exporter.InMemoryFileSystem.withNoParams;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * @author Russell Gold
 */
public class LiveConfigurationTest {
    private static final String CONFIGURATION =
            "host: " + HOST + "\n" +
            "port: " + PORT + "\n" +
            "queries:\n" + "" +
            "- groups:\n" +
            "    prefix: new_\n" +
            "    key: name\n" +
            "    values: [sample1, sample2]\n";

    private static final String ADDED_CONFIGURATION =
            "host: " + HOST + "\n" +
            "port: " + PORT + "\n" +
            "queries:\n" + "" +
            "- people:\n" +
            "    key: name\n" +
            "    values: [age, sex]\n";

    private static final String COMBINED_CONFIGURATION =
            "host: " + HOST + "\n" +
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

    @Before
    public void setUp() throws Exception {
        InMemoryFileSystem.install();
        ConfigurationUpdaterStub.install();
        LiveConfiguration.setServer(HttpServletRequestStub.createPostRequest());
    }

    @After
    public void tearDown() {
        InMemoryFileSystem.uninstall();
        ConfigurationUpdaterStub.uninstall();
    }

    @Test
    public void afterInitCalled_haveQueries() {
        init(CONFIGURATION);

        assertThat(LiveConfiguration.hasQueries(), is(true));
    }

    private void init(String configuration) {
        InMemoryFileSystem.defineResource(LiveConfiguration.CONFIG_YML, configuration);
        LiveConfiguration.init(withNoParams());
    }

    @Test
    public void whenInitNotCalled_haveNoQueries() {
        assertThat(LiveConfiguration.hasQueries(), is(false));
    }

    @Test
    public void whenInitCalledWithNoConfig_haveNoQueries() {
        LiveConfiguration.init(withNoParams());
        
        assertThat(LiveConfiguration.hasQueries(), is(false));
    }

    @Test
    public void afterInitCalled_haveExpectedConfiguration() {
        init(CONFIGURATION);

        assertThat(LiveConfiguration.asString(), equalTo(CONFIGURATION));
    }

    @Test
    public void afterInitCalledTwice_haveFirstConfiguration() {
        init(CONFIGURATION);
        init(ADDED_CONFIGURATION);

        assertThat(LiveConfiguration.asString(), equalTo(CONFIGURATION));
    }

    @Test
    public void afterInitTimestampIsZero() {
        init(CONFIGURATION);
        
        assertThat(LiveConfiguration.getTimestamp(), equalTo(0L));
    }

    @Test
    public void afterReplaceQueryCalled_configurationIsUpdated() throws Exception {
        init(CONFIGURATION);

        LiveConfiguration.replaceConfiguration(toConfiguration(ADDED_CONFIGURATION));

        assertThat(LiveConfiguration.asString(), equalTo(ADDED_CONFIGURATION));
    }

    @SuppressWarnings("SameParameterValue")
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
    public void whenSharedTimestampIndicatesNewConfiguration_updateLiveConfiguration() {
        init(CONFIGURATION);

        long newTimestamp = LiveConfiguration.getTimestamp() + 1;
        ConfigurationUpdaterStub.newConfiguration(newTimestamp, ADDED_CONFIGURATION);

        LiveConfiguration.updateConfiguration();

        assertThat(LiveConfiguration.asString(), equalTo(ADDED_CONFIGURATION));
        assertThat(LiveConfiguration.getTimestamp(), equalTo(newTimestamp));
    }

    @Test
    public void whenSharedTimestampIndicatesHaveLatestConfiguration_dontUpdateLiveConfiguration() {
        init(CONFIGURATION);

        long newTimestamp = LiveConfiguration.getTimestamp();
        ConfigurationUpdaterStub.newConfiguration(newTimestamp, ADDED_CONFIGURATION);

        LiveConfiguration.updateConfiguration();

        assertThat(LiveConfiguration.asString(), equalTo(CONFIGURATION));
    }

    @Test
    public void whenConfigurationSpecifiesSynchronization_installHttpBasedUpdater() throws Exception {
        init(CONFIGURATION_WITH_SYNC);

        assertThat(getConfigurationUpdater(), instanceOf(ConfigurationUpdaterImpl.class));
    }

    private ConfigurationUpdater getConfigurationUpdater() throws NoSuchFieldException {
        Memento memento = StaticStubSupport.preserve(LiveConfiguration.class, "updater");
        return memento.getOriginalValue();
    }

    @Test
    public void whenConfigurationSpecifiesSynchronization_configureUpdater() throws Exception {
        init(CONFIGURATION_WITH_SYNC);

        ConfigurationUpdaterImpl configurationUpdater = (ConfigurationUpdaterImpl) getConfigurationUpdater();
        assertThat(configurationUpdater.getRepeaterUrl(), equalTo(SYNC_URL));
        assertThat(configurationUpdater.getRefreshInterval(), equalTo(REFRESH_INTERVAL));
    }
}
