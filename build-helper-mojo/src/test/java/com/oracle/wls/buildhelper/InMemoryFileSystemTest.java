// Copyright (c) 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.buildhelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryFileSystemTest {
  static final String CREATED_FILE = "/top/of/the/system/MyFile.txt";
  static final String CREATED_FILE_CONTENT = "some text";

  private final InMemoryFileSystem fileSystem = InMemoryFileSystem.createInstance();
  private final Path SLASH = toPath("/");

  @BeforeEach
  void setUp() {
    fileSystem.defineFile(CREATED_FILE, CREATED_FILE_CONTENT);
  }

  @Test
  void canCreateDummyFile() {
    assertThat(Files.exists(toPath(CREATED_FILE)), is(true));
  }

  @Test
  void canReadDummyFile() throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(toPath(CREATED_FILE))) {
      assertThat(reader.readLine(), equalTo(CREATED_FILE_CONTENT));
    };
  }

  @Test
  void whenParentDirectoriesAbsent_cannotCreateSubdirectory() {
    assertThrows(IOException.class, () -> Files.createDirectory(fileSystem.getPath("/x/y")));
  }

  @Test
  void relativize_dropsParentPath() {
    assertThat(toPath("/").relativize(toPath("/a/b/c")), equalTo(toPath("a/b/c")));
  }

  @Test
  void pathReturnsNameCount() {
    assertThat(toPath("a/b/c").getNameCount(), equalTo(3));
  }

  @Test
  void pathReturnsNameFromIndex() {
    Path path = toPath("a/b/c");

    assertThat(path.getName(0), equalTo(toPath("a")));
    assertThat(path.getName(1), equalTo(toPath("b")));
    assertThat(path.getName(2), equalTo(toPath("c")));
  }

  @Test
  void resolve_addsToParent() {
    assertThat(toPath("/").resolve("a"), equalTo(toPath("/a")));
    assertThat(toPath("/a").resolve("b"), equalTo(toPath("/a/b")));
    assertThat(toPath("/a").resolve("/b"), equalTo(toPath("/a/b")));
  }

  @Test
  void rootDirIsSlash() {
    assertThat(toPath("/a/b/c").getRoot(), equalTo(SLASH));
  }

  @Test
  void getParentDirectory() {
    assertThat(toPath("/a/b/c").getParent(), equalTo(toPath("/a/b")));
  }

  @Test
  void parentForRelativePath_isNull() {
    assertThat(toPath("a").getParent(), nullValue());
  }

  @Test
  void parentForTopLevelSubdirectory_isSlash() {
    assertThat(toPath("/subdir").getParent(), equalTo(SLASH));
  }

  @Test
  void parentForSlash_isNull() {
    assertThat(SLASH.getParent(), nullValue());
  }

  private Path toPath(String filePath) {
    return fileSystem.getPath(filePath);
  }

  @Test
  void canCreateTopLevelSubdirectory() throws IOException {
    Path topPath = toPath("/top");

    Files.createDirectory(topPath);

    assertThat(Files.exists(topPath), is(true));
    assertThat(Files.isDirectory(topPath), is(true));
  }

  @Test
  void whenParentDirectoriesPresent_createSubdirectory() throws IOException {
    Path newDirectory = toPath("/a/b/c");

    Files.createDirectories(newDirectory);

    assertThat(Files.exists(newDirectory), is(true));
    assertThat(Files.isDirectory(newDirectory), is(true));
  }

  @Test
  void whenParentDirectoriesAbsent_cannotOpenFileForOutput() {
    assertThrows(IOException.class, () -> writeToFile("/a/b/c/BadFile.txt", "somethingToWrite"));
  }

  private void writeToFile(String filePath, String contents) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(toPath(filePath))) {
      writer.write(contents);
    }
  }

  @Test
  void whenParentDirectoryPresent_openFileForOutput() throws IOException {
    Files.createDirectories(fileSystem.getPath("/a/b/c"));

    writeToFile("/a/b/c/NewFile.txt", "something that works");
  }
}