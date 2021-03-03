// Copyright (c) 2017, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;

import static com.meterware.simplestub.Stub.createStrictStub;

public abstract class ConfigurationUpdaterStub implements ConfigurationUpdater {
    private static Memento memento;
    private static ConfigurationUpdaterStub updaterStub;

    private String sharedConfiguration;
    private ConfigurationUpdate update;

    protected ConfigurationUpdaterStub() {
    }

    public static void install() throws NoSuchFieldException {
        updaterStub = createStrictStub(ConfigurationUpdaterStub.class);
        memento = StaticStubSupport.install(LiveConfiguration.class, "updater", updaterStub);
    }

    public static void uninstall() {
        memento.revert();
    }

    static String getSharedConfiguration() {
        return updaterStub.sharedConfiguration;
    }

    public static void newConfiguration(long newTimestamp, String configuration) {
        updaterStub.update = new ConfigurationUpdate(newTimestamp, configuration);
    }

    @Override
    public long getLatestConfigurationTimestamp() {
        return update == null ? 0 : update.getTimestamp();
    }

    @Override
    public void shareConfiguration(String configuration) {
        sharedConfiguration = configuration;
        update = new ConfigurationUpdate(1, configuration);
    }

    @Override
    public ConfigurationUpdate getUpdate() {
        return update;
    }
}
