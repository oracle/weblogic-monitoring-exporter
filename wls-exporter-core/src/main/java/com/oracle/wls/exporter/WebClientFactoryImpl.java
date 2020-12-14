// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

/**
 * The production implementation of the web client factory interface.
 *
 * @author Russell Gold
 */
public class WebClientFactoryImpl implements WebClientFactory {

  static final String APACHE_BASED_CLIENT = "com.oracle.wls.exporter.WebClientImpl";
  static final String JDK_BASED_CLIENT = "com.oracle.wls.exporter.WebClient8Impl";
  static final String[] CLIENT_CLASS_NAMES = {APACHE_BASED_CLIENT, JDK_BASED_CLIENT};

  static Constructor<? extends WebClient> clientConstructor;
  static Function<String,Class<? extends WebClient>> loadClientClass = WebClientFactoryImpl::getClientClass;

  static {
    clientConstructor = getClientConstructor();
  }

  static Constructor<? extends WebClient> getClientConstructor() {
    for (String className : CLIENT_CLASS_NAMES) {
      try {
        return getClientConstructor(className);
      } catch (NoClassDefFoundError | NoSuchMethodException ignored) {
      }
    }
    
    throw new IllegalStateException("Unable to select client constructor");
  }

  private static Constructor<? extends WebClient> getClientConstructor(String clientClassName)
        throws NoSuchMethodException {
    return loadClientClass.apply(clientClassName).getDeclaredConstructor();
  }

  @SuppressWarnings("unchecked")
  private static Class<? extends WebClient> getClientClass(String clientClassName) {
    try {
      return (Class<? extends WebClient>) Class.forName(clientClassName);
    } catch (ClassNotFoundException e) {
      throw new NoClassDefFoundError(e.getMessage());
    }
  }

  private static String getClientClassName() {
    return isApacheClientAvailable() ? APACHE_BASED_CLIENT : JDK_BASED_CLIENT;
  }

  private static boolean isApacheClientAvailable() {
    try {
      Class.forName("org.apache.http.HttpEntity");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public WebClient createClient() {
    try {
      return clientConstructor.newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException("Unable to instantiate client class", e);
    }
  }


}
