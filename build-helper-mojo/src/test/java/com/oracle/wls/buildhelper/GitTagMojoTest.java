// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.buildhelper;

import com.meterware.simplestub.Stub;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class GitTagMojoTest {
  private final InMemoryFileSystem inMemoryFileSystem = InMemoryFileSystem.createInstance();
  private final GitTagExecutorStub stub = Stub.createStub(GitTagExecutorStub.class, inMemoryFileSystem);
  private final GitTagMojo mojo = new GitTagMojo(stub);
  private final File outputFile = new File("/a/b/v.properties");
  private final Path outputPath = inMemoryFileSystem.getPath(outputFile.getAbsolutePath());

  private MojoTestSupport mojoTestSupport;

  @BeforeEach
  void setUp() throws Exception {
    mojoTestSupport = new MojoTestSupport(GitTagMojo.class);
    setMojoParameter("outputFile", outputFile);
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
    assertThat(mojoTestSupport.getClassAnnotation().get("name"), equalTo("gitVersion"));
  }

  @Test
  void mojoAnnotatedWithDefaultPhase() {
    assertThat(mojoTestSupport.getClassAnnotation().get("defaultPhase"), equalTo(LifecyclePhase.PREPARE_PACKAGE));
  }

  @Test
  void hasRequiredOutputDirectoryParameter() throws NoSuchFieldException {
    assertThat(mojoTestSupport.getParameterField("outputFile").getType(), equalTo(File.class));
    assertThat(mojoTestSupport.getParameterAnnotation("outputFile").get("required"), is(true));
    assertThat(mojoTestSupport.getParameterAnnotation("outputFile").get("defaultValue"), equalTo("${project.build.outputDirectory}/version.properties"));
  }

  @Test
  void whenMojoExecuted_runCorrectGitCommand() throws Exception {
    mojo.execute();

    assertThat(stub.getExecutedCommand(), equalTo("git describe --tag"));
  }

  @Test
  void whenMojoExecuted_generateOutputFile() throws Exception {
    mojo.execute();

    assertThat(Files.exists(outputPath), is(true));
  }

  @Test
  void whenGitResponseIsVersionPlusHistory_createVersionProperty() throws Exception {
    stub.setCommandResponse("v3.3.5-3-946-gcb4385f3aa");

    mojo.execute();

    assertThat(inMemoryFileSystem.getContents(outputFile.getAbsolutePath()),
          containsString("version=gcb4385f3aa (946 commits since v3.3.5-3)"));
  }

  @Test
  void whenGitResponseIsVersionOnly_createVersionProperty() throws Exception {
    stub.setCommandResponse("v3.4-1");

    mojo.execute();

    assertThat(inMemoryFileSystem.getContents(outputFile.getAbsolutePath()), containsString("version=v3.4-1"));
  }

  @SuppressWarnings("SameParameterValue")
  private void setMojoParameter(String fieldName, Object value) throws Exception {
    Field field = mojo.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(mojo, value);
  }

  static abstract class GitTagExecutorStub implements GitTagExecutor {
    private static final String DEFAULT_RESPONSE = "v3.3.5";
    private final InMemoryFileSystem inMemoryFileSystem;
    private String commandLine;
    private String commandResponse = DEFAULT_RESPONSE;

    GitTagExecutorStub(InMemoryFileSystem inMemoryFileSystem) {
      this.inMemoryFileSystem = inMemoryFileSystem;
    }

    void setCommandResponse(String commandResponse) {
      this.commandResponse = commandResponse;
    }

    String getExecutedCommand() {
      return commandLine;
    }

    @Override
    public String runCommand(String... commandLineStrings) {
      this.commandLine = String.join(" ", commandLineStrings);
      return commandResponse;
    }

    @Override
    public Path toPath(File file) {
      return inMemoryFileSystem.getPath(file.getAbsolutePath());
    }
  }
}
