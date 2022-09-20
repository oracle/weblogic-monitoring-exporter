// Copyright (c) 2020, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.buildhelper;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import com.meterware.simplestub.SystemPropertySupport;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileCopyMojoTest {
  private final InMemoryFileSystem inMemoryFileSystem = InMemoryFileSystem.createInstance();
  private final CopyExecutorStub copyExecutorStub = new CopyExecutorStub(inMemoryFileSystem);
  private final FileCopyMojo mojo = new FileCopyMojo();
  private final List<Memento> mementos = new ArrayList<>();
  private MojoTestSupport mojoTestSupport;

  @BeforeEach
  public void setUp() throws Exception {
    mojoTestSupport = new MojoTestSupport(FileCopyMojo.class);
    mementos.add(StaticStubSupport.install(FileCopyMojo.class, "executor", copyExecutorStub));
    mementos.add(SystemPropertySupport.preserve("user.dir"));
  }

  @AfterEach
  public void tearDown() {
    mementos.forEach(Memento::revert);
  }

  @Test
  void helperImplementsMojo() {
    assertThat(mojo, Matchers.instanceOf(AbstractMojo.class));
  }

  @Test
  void mojoHasGoalAnnotation() {
    assertThat(mojoTestSupport.getClassAnnotation(), notNullValue());
  }

  @Test
  void mojoAnnotatedWithName() {
    assertThat(mojoTestSupport.getClassAnnotation().get("name"), equalTo("copy"));
  }

  @Test
  void mojoAnnotatedWithDefaultPhase() {
    assertThat(mojoTestSupport.getClassAnnotation().get("defaultPhase"), equalTo(PROCESS_RESOURCES));
  }

  @Test
  void hasRequiredSourceFileParameter() throws NoSuchFieldException {
    assertThat(mojoTestSupport.getParameterField("sourceFile").getType(), equalTo(String.class));
    assertThat(mojoTestSupport.getParameterAnnotation("sourceFile").get("required"), is(true));
  }

  @Test
  void hasAnnotatedTargetFileField_withNoDefault() throws NoSuchFieldException {
    assertThat(mojoTestSupport.getParameterField("targetFile").getType(), equalTo(File.class));
    assertThat(mojoTestSupport.getParameterAnnotation("targetFile").get("defaultValue"), nullValue());
  }

  @Test
  void targetFileField_isRequired() throws NoSuchFieldException {
    assertThat(mojoTestSupport.getParameterAnnotation("targetFile").get("required"), is(true));
  }

  @Test
  void hasAnnotatedUserDirFileField_withNullDefault() throws NoSuchFieldException {
    assertThat(mojoTestSupport.getParameterField("userDir").getType(), equalTo(File.class));
    assertThat(mojoTestSupport.getParameterAnnotation("userDir").get("defaultValue"), nullValue());
  }

  @Test
  void userDirField_isNotRequired() throws NoSuchFieldException {
    assertThat(mojoTestSupport.getParameterAnnotation("userDir").get("required"), nullValue());
  }

  @Test
  void whenSourceAndTargetAbsolute_useAbsolutePaths() throws Exception {
    setMojoParameter("sourceFile", "/root/source");
    setMojoParameter("targetFile", new File("/root/target"));
    inMemoryFileSystem.defineFile("/root/source", "**CONTENT**");

    mojo.execute();

    assertThat(inMemoryFileSystem.getContents("/root/target"), equalTo("**CONTENT**"));
  }

  @Test
  void whenSourcePathIsRelative_computeAbsoluteRelativeToUserDir() throws Exception {
    setMojoParameter("sourceFile", "source");
    setMojoParameter("targetFile", new File("/root/target"));
    setMojoParameter("userDir", new File("/root/nested"));
    inMemoryFileSystem.defineFile("/root/nested/source", "**CONTENT**");

    mojo.execute();

    assertThat(inMemoryFileSystem.getContents("/root/target"), equalTo("**CONTENT**"));
  }

  @Test
  void whenSourcePathIsRelativeAndNoUserDir_useSystemProperty() throws Exception {
    System.setProperty("user.dir", "/user");
    setMojoParameter("sourceFile", "source");
    setMojoParameter("targetFile", new File("/root/target"));
    inMemoryFileSystem.defineFile("/user/source", "**CONTENT**");

    mojo.execute();

    assertThat(inMemoryFileSystem.getContents("/root/target"), equalTo("**CONTENT**"));
  }

  @Test
  void whenUnableToCopy_reportFailure() throws Exception {
    System.setProperty("user.dir", "/user");
    setMojoParameter("sourceFile", "source");
    setMojoParameter("targetFile", new File("/root/target"));
    inMemoryFileSystem.throwExceptionOnAccess("/root/target");

    assertThrows(MojoExecutionException.class, mojo::execute);
  }

  private void setMojoParameter(String fieldName, Object value) throws Exception {
    Field field = mojo.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(mojo, value);
  }

  static class CopyExecutorStub implements CopyExecutor {

    private final InMemoryFileSystem inMemoryFileSystem;

    public CopyExecutorStub(InMemoryFileSystem inMemoryFileSystem) {
      this.inMemoryFileSystem = inMemoryFileSystem;
    }

    @Override
    public Path toPath(File file) {
      return inMemoryFileSystem.getPath(file.getAbsolutePath());
    }
  }

}
