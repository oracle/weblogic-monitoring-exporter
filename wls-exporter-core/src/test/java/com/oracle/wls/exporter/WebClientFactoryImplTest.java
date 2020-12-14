// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class WebClientFactoryImplTest {

  private final List<Memento> mementos = new ArrayList<>();
  private Function<String, Class<? extends WebClient>> apacheClassesMissing = WebClientFactoryImplTest::reportApacheClassesMissing;

  static Class<? extends WebClient> reportApacheClassesMissing(String className) {
    if (className.equals(WebClientImpl.class.getName()))
      throw new NoClassDefFoundError("a class");
    else if (className.equals(WebClient8Impl.class.getName()))
      return WebClient8Impl.class;
    else
      throw new RuntimeException("Unexpected client class name: " + className);
  }

  @After
  public void tearDown() {
    mementos.forEach(Memento::revert);
  }

  @Test
  public void whenApacheClientDependentClassesFound_selectApacheClient() {
    assertThat(WebClientFactoryImpl.getClientConstructor().getDeclaringClass(), sameInstance(WebClientImpl.class));
  }

  @Test
  public void whenApacheClientDependentClassesMissing_selectJKD8Client() throws NoSuchFieldException {
    mementos.add(StaticStubSupport.install(WebClientFactoryImpl.class, "loadClientClass", apacheClassesMissing));

    assertThat(WebClientFactoryImpl.getClientConstructor().getDeclaringClass(), sameInstance(WebClient8Impl.class));
  }

}