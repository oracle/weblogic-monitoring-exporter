package io.prometheus.wls.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.prometheus.wls.rest.InMemoryFileSystem.withNoParams;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class LiveConfigurationTest {
    private static final String CONFIGURATION = "---\n" +
            "host: localhost\n" +
            "port: 7001\n" +
            "queries:\n" + "" +
            "- groups:\n" +
            "    prefix: new_\n" +
            "    key: name\n" +
            "    values: [sample1, sample2]\n";

    private static final String ADDED_CONFIGURATION = "---\n" +
            "host: localhost\n" +
            "port: 7001\n" +
            "queries:\n" + "" +
            "- people:\n" +
            "    key: name\n" +
            "    values: [age, sex]\n";

    @Before
    public void setUp() throws Exception {
        InMemoryFileSystem.install();
    }

    @After
    public void tearDown() throws Exception {
        InMemoryFileSystem.uninstall();
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
    public void whenInitCalledAfterReplace_ignoreIt() throws Exception {
    }
}