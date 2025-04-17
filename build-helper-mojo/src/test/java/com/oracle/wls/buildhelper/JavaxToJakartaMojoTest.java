// Copyright (c) 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.buildhelper;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import com.meterware.simplestub.SystemPropertySupport;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaxToJakartaMojoTest {
  static final String ROOT = "/project";
  static final String SOURCE_ROOT = ROOT + "/src/main/java";
  static final String GENERATED_SOURCE_ROOT = ROOT + "/target/generated-sources/jakarta";
  static final String SERVLET_1_INPUT_PATH = SOURCE_ROOT + "/a/b/c/javax/Servlet1.java";
  static final String SERVLET_1_OUTPUT_PATH = GENERATED_SOURCE_ROOT + "/a/b/c/jakarta/Servlet1.java";
  static final String SERVLET_2_INPUT_PATH = SOURCE_ROOT + "/d/e/f/javax/Servlet2.java";
  static final String SERVLET_2_OUTPUT_PATH = GENERATED_SOURCE_ROOT + "/d/e/f/jakarta/Servlet2.java";
  private final InMemoryFileSystem inMemoryFileSystem = InMemoryFileSystem.createInstance();
  private final JavaxToJakartaMojo mojo = new JavaxToJakartaMojo();
  private final List<Memento> mementos = new ArrayList<>();

  private MojoTestSupport mojoTestSupport;
  private final Path projectRoot = toPath(ROOT);
  private final MavenProject mavenProject = new MavenProject();
  Function<String, Path> toPath = inMemoryFileSystem::getPath;

  @BeforeEach
  void setUp() throws Exception {
    mojoTestSupport = new MojoTestSupport(JavaxToJakartaMojo.class);
    mojoTestSupport.getParameterField("compileSourceRoots").set(mojo, List.of(SOURCE_ROOT));
    mojoTestSupport.getParameterField("generatedSourcesDirectory").set(mojo, new File(GENERATED_SOURCE_ROOT));
    mojoTestSupport.getParameterField("project").set(mojo, mavenProject);

    inMemoryFileSystem.defineFile(SERVLET_1_INPUT_PATH, "file 1\n with javax.servlet");
    inMemoryFileSystem.defineFile(SERVLET_2_INPUT_PATH, "file 2\n with javax.servlet");
    inMemoryFileSystem.defineFile(ROOT + "/src/main/java/a/b/RandomFile.java", "file without ee");
    inMemoryFileSystem.defineFile(ROOT + "/src/main/java/a/b/TrickFile.java", "file with javax.net");

    mementos.add(SystemPropertySupport.preserve("user.dir"));
    mementos.add(StaticStubSupport.install(JavaxToJakartaMojo.class, "toPath", toPath));
  }

  @AfterEach
  void tearDown() {
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
    assertThat(mojoTestSupport.getClassAnnotation().get("name"), equalTo("transform"));
  }

  @Test
  void mojoAnnotatedWithDefaultPhase() {
    assertThat(mojoTestSupport.getClassAnnotation().get("defaultPhase"), equalTo(GENERATE_SOURCES));
  }

  @Test
  void failsIfFileContainingJavaxServletPackageIsNotInPackageEndingWithJavax() {
    inMemoryFileSystem.defineFile(ROOT + "/src/main/java/a/b/c/Servlet1.java", "bad file with javax.servlet");

    MojoFailureException mojoFailureException = assertThrows(MojoFailureException.class, mojo::execute);

    assertThat(mojoFailureException.getMessage(), both(containsString("javax")).and(containsString("Servlet1")));
  }

  @Test
  void addsGeneratedSourceDirectory() throws MojoExecutionException, MojoFailureException {
    mojo.execute();

    assertThat(mavenProject.getCompileSourceRoots(), hasItem(GENERATED_SOURCE_ROOT));
  }

  @Test
  void findsFilesToTransform() throws MojoFailureException {
    assertThat(mojo.getFilesToTransform().map(this::toRelativePath).map(Path::toString).toList(),
          containsInAnyOrder("a/b/c/javax/Servlet1.java", "d/e/f/javax/Servlet2.java"));
  }

  private Path toRelativePath(Path p) {
    return getStandardCompileSourceRoot().relativize(p);
  }

  @Test
  void determinesOutputFiles() {
    assertThat(mojo.getOutputFile(toPath(SERVLET_1_INPUT_PATH)), equalTo(toPath(SERVLET_1_OUTPUT_PATH)));
    assertThat(mojo.getOutputFile(toPath(SERVLET_2_INPUT_PATH)), equalTo(toPath(SERVLET_2_OUTPUT_PATH)));
  }

  private Path toPath(String path) {
    return inMemoryFileSystem.getPath(path);
  }

  @Test
  void createsJakartaFiles() throws MojoExecutionException, MojoFailureException {
    mojo.execute();

    assertThat(inMemoryFileSystem.getContents(SERVLET_1_OUTPUT_PATH), equalTo("file 1\n with jakarta.servlet\n"));
    assertThat(inMemoryFileSystem.getContents(SERVLET_2_OUTPUT_PATH), equalTo("file 2\n with jakarta.servlet\n"));
  }

  private Path getStandardCompileSourceRoot() {
    return projectRoot.resolve("src").resolve("main").resolve("java");
  }
}
