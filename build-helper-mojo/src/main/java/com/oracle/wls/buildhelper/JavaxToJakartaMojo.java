// Copyright (c) 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.buildhelper;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Supports generation of web applications that work on both javax-dependent and jakarta-dependent WebLogic Server
 * instances. It depends on several things:
 * 1. The Exporter web application uses annotations rather than an XML file to configure its servlets,
 * 2. WebLogic will ignore servlets whose dependencies it cannot load
 * 3. Common code in this application does not depend on classes that depend on the EE version.
 * <br/>
 * The plugin will look for source files that reference javax.servlet; it will report a failure if any such
 * files are in packages whose last element is not "javax." It will then generate corresponding source files
 * that reference the corresponding jakarta.servlet classes and place them in a package with "jakarta" instead
 * of "javax" in a separate directory, and add the directory to the compiler source roots.
 */
@Mojo(name = "transform", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class JavaxToJakartaMojo extends AbstractMojo {
  /**
   * The source directories containing the sources to be processed.
   */
  @Parameter(defaultValue = "${project.compileSourceRoots}", required = true)
  List<String> compileSourceRoots;

  /**
   * Specify where to place generated jakarta-based files created by this mojo.
   */
  @Parameter(defaultValue = "${project.build.directory}/generated-sources/jakarta")
  File generatedSourcesDirectory;

  /**
   * The project used as an interface to Maven internals.
   */
  @Parameter(defaultValue = "${project}", readonly = true)
  MavenProject project;

  static Function<String, Path> toPath = Path::of;


  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    project.addCompileSourceRoot(generatedSourcesDirectory.getPath());

    try {
      getFilesToTransform().forEach(this::transformFile);
    } catch (RuntimeException e) {
      throw new MojoExecutionException(e);
    }
  }

  private void transformFile(Path inputFile) {
    Path outputFile = getOutputFile(inputFile);
    try {
      Files.createDirectories(outputFile.getParent());
      transformFile(inputFile, outputFile);
    } catch (IOException e) {
      throw new RuntimeException("Unable to transform files", e);
    }
  }

  private static void transformFile(Path inputFile, Path outputFile) throws IOException {
    try (BufferedWriter bufferedWriter = Files.newBufferedWriter(outputFile);
         Stream<String> lines = Files.lines(inputFile)) {
      lines.forEach(l -> transformLine(l, bufferedWriter));
    }
  }

  private static void transformLine(String line, BufferedWriter bufferedWriter) {
    try {
      bufferedWriter.write(line.replaceAll("javax", "jakarta"));
      bufferedWriter.newLine();
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private Path getGeneratedSourceRoot() {
    return toPath.apply(generatedSourcesDirectory.getPath());
  }

  Stream<Path> getFilesToTransform() throws MojoFailureException {
    final List<Path> selectedFiles = new ArrayList<>();

    try (Stream<Path> files = Files.walk(getCompileSourceRoot())) {
      files.filter(Files::isRegularFile).filter(this::containsTransformedJavaxPackage).forEach(selectedFiles::add);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    verifySelectedFilesHaveJavaxPackage(selectedFiles);
    return selectedFiles.stream();
  }

  private void verifySelectedFilesHaveJavaxPackage(List<Path> selectedFiles) throws MojoFailureException {
    List<String> badPaths = selectedFiles.stream().filter(this::lacksJavaxFinalPackage).map(Path::toString).toList();

    if (!badPaths.isEmpty())
      throw new MojoFailureException("Cannot do transformation. The following classes reference javax.servlet packages " +
            "but are not in a package name ending in .javax: " + String.join(", ", badPaths));
  }

  private boolean lacksJavaxFinalPackage(Path path) {
    return !path.getParent().endsWith("javax");
  }

  private Path toRelativePath(Path p) {
    return getCompileSourceRoot().relativize(p);
  }

  private Path getCompileSourceRoot() {
    return toPath.apply(compileSourceRoots.get(0));
  }

  private boolean containsTransformedJavaxPackage(Path path) {
    try {
      return Files.readAllLines(path).stream().anyMatch(s -> s.contains("javax.servlet"));
    } catch (IOException e) {
      return false;
    }
  }

  public Path getOutputFile(Path inputPath) {
    final Path fileName = inputPath.getFileName();
    final Path relativePath = toRelativePath(inputPath).getParent().getParent().resolve("jakarta");
    return getGeneratedSourceRoot().resolve(relativePath).resolve(fileName);
  }
}
