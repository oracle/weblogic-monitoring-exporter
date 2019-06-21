package io.prometheus.wls.rest;
/*
 * Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
import com.google.common.collect.ImmutableMap;
import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.meterware.simplestub.Stub.createStrictStub;

/**
 * @author Russell Gold
 */
class InMemoryFileSystem {
    private static Map<String, InputStream> resources;
    private static List<Memento> mementos = new ArrayList<>();

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

    static void defineResource(String filePath, String contents) {
        resources.put(filePath, toInputStream(contents));
    }

    private static InputStream toInputStream(String contents) {
        return new ByteArrayInputStream(contents.getBytes());
    }

    abstract static class ServletConfigStub implements ServletConfig {

        private Map<String, String> params;
        private ServletContext context;

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
