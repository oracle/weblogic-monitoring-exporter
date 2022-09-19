// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.buildhelper;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mojo(name = "gitVersion", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class GitTagMojo extends AbstractMojo {

  @Parameter(required = true, defaultValue = "${project.build.outputDirectory}/version.properties")
  private File outputFile;
  
  private final GitTagExecutor executor;

  GitTagMojo(GitTagExecutor executor) {
    this.executor = executor;
  }

  public GitTagMojo() {
    this(new GitTagExecutorImpl());
  }

  @Override
  public void execute() throws MojoExecutionException {
    try {
      final String versionString = toVersionString(executor.runCommand("git", "describe", "--tag"));
      final Path path = executor.toPath(outputFile);
      Files.createDirectories(path.getParent());
      Files.write(path, createProperties(versionString));
    } catch (IOException e) {
      throw new MojoExecutionException("Error writing " + outputFile + ": " + e );
    }

  }

  private List<String> createProperties(String versionString) {
    return Collections.singletonList("version=" + versionString);
  }

  // parses the result of 'git describe --tag' to describe the current code version/
  String toVersionString(String gitResponse) {
    final String[] segments = gitResponse.split("-");
    if (segments.length == 0)
      return "UNKNOWN";
    else if (segments.length == 1 || segments.length == 2)
      return gitResponse;
    else
      return formatVersionString(segments);
  }

  private String formatVersionString(String[] segments) {
    final String commit = segments[segments.length - 1];
    final String numCommits = segments[segments.length - 2];
    final String versionParts = String.join("-", Arrays.copyOfRange(segments, 0, segments.length - 2));
    return formatVersionString(commit, numCommits, versionParts);
  }

  private String formatVersionString(String commit, String numCommits, String versionParts) {
    return commit + " (" + numCommits + " commits since " + versionParts + ")";
  }
}
