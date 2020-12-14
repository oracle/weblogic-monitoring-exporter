// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import com.google.common.collect.ImmutableMap;
import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;

import static com.meterware.simplestub.Stub.createStrictStub;

/**
 * @author Russell Gold
 */
class InMemoryFileSystem {
    private static Map<String, InputStream> resources;
    private static final List<Memento> mementos = new ArrayList<>();

    static void install() throws NoSuchFieldException {
        resources = new HashMap<>();
        mementos.add(StaticStubSupport.install(LiveConfiguration.class, "config", null));
        mementos.add(StaticStubSupport.install(LiveConfiguration.class, "timestamp", null));
    }

    static void uninstall() {
        for (Memento memento : mementos) memento.revert();
    }

    static ServletConfig withNoParams() {
        return createStrictStub(ServletConfigStub.class, ImmutableMap.of());
    }

    @SuppressWarnings("SameParameterValue")
    static void defineResource(String filePath, String contents) {
        resources.put(filePath, toInputStream(contents));
    }

    private static InputStream toInputStream(String contents) {
        return new ByteArrayInputStream(contents.getBytes());
    }

    abstract static class ServletConfigStub implements ServletConfig {

        private final Map<String, String> params;
        private final ServletContext context;

        ServletConfigStub(Map<String, String> params) {
            this.params = params;
            context = createStrictStub(ServletContextStub.class);
        }

        @Override
        public String getInitParameter(String s) {
            return params.get(s);
        }

        @Override
        public ServletContext getServletContext() {
            return context;
        }

    }

    abstract static class ServletContextStub implements ServletContext {

        @Override
        public InputStream getResourceAsStream(String path) {
            return !LiveConfiguration.CONFIG_YML.equals(path) ? null : InMemoryFileSystem.resources.get(path);
        }
    }
}
