// Copyright (c) 2018, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.buildhelper;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

import static com.meterware.simplestub.Stub.createStrictStub;

/**
 * An in-memory implementation of the file system to allow unit testing of file operations without touching the
 * real file system. This offers both complete control of what the unit tests access, and makes them much faster.
 * Since it is used only for unit testing, we can assume Unix-style paths.
 */
public abstract class InMemoryFileSystem extends FileSystem {
  private static final String DIRECTORY_OBJECT = "**reserved string: directory**";
  private static final String SEPARATOR = "/";

  private static InMemoryFileSystem instance;
  private final FileSystemProviderStub provider = createStrictStub(FileSystemProviderStub.class);

  public static InMemoryFileSystem createInstance() {
    instance = createStrictStub(InMemoryFileSystem.class);
    instance.defineFile(SEPARATOR, DIRECTORY_OBJECT);
    return instance;
  }

  @SuppressWarnings("unused")
  public void defineFile(File file, String contents) {
    instance.defineFileContents(file.getPath(), contents);
  }

  public void defineFile(String filePath, String contents) {
    instance.defineFileContents(filePath, contents);
  }

  public String getContents(String filePath) {
    return provider.fileContents.get(filePath);
  }

  public void throwExceptionOnAccess(String path) {
    provider.throwExceptionOnAccess = path;
  }

  public @Nonnull Path getPath(@Nonnull String first, @Nonnull String... more) {
    return PathStub.createPathStub(createPathString(first, more));
  }

  private String createPathString(String first, String[] more) {
    return more.length == 0 ? first : first + SEPARATOR + String.join(SEPARATOR, more);
  }

  @Override
  public FileSystemProvider provider() {
    return provider;
  }

  private void defineFileContents(String filePath, String contents) {
    if (!filePath.startsWith(SEPARATOR)) throw new RuntimeException("Must specify full absolute path");
    provider.fileContents.put(filePath, contents);

    String directoryPath = "";
    for (String name : filePath.split(SEPARATOR)) {
      if (name.isEmpty()) continue;
      directoryPath += (SEPARATOR + name);
      if (!provider.fileContents.containsKey(directoryPath))
        provider.fileContents.put(directoryPath, DIRECTORY_OBJECT);
    }
  }


  enum PathType {
    DIRECTORY {
      @Override
      boolean isDirectory() {
        return true;
      }

      @Override
      boolean matches(Object o) {
        return DIRECTORY_OBJECT == o;
      }
    },
    FILE {
      @Override
      boolean isRegularFile() {
        return true;
      }

      @Override
      boolean matches(Object o) {
        return o instanceof String;
      }
    };

    boolean isDirectory() {
      return false;
    }

    boolean isRegularFile() {
      return false;
    }

    abstract boolean matches(Object o);

    static PathType getPathTypeFor(Object object) {
      for (PathType type : PathType.values())
        if (type.matches(object)) return type;

      return null;
    }
  }

  abstract static class PathStub implements Path {
    private final String filePath;

    PathStub(String filePath) {
      this.filePath = filePath;
    }

    static PathStub createPathStub(String pathString) {
      return createStrictStub(PathStub.class, pathString);
    }

    @Override
    public @Nonnull FileSystem getFileSystem() {
      return instance;
    }

    @Override
    public Path getFileName() {
      return createPathStub(getLastElement());
    }

    protected String getLastElement() {
      final int beginIndex = filePath.lastIndexOf(SEPARATOR);
      return beginIndex < 0 ? filePath : filePath.substring(beginIndex + 1);
    }

    @Override
    public Path getParent() {
      final int beginIndex = filePath.lastIndexOf(SEPARATOR);
      if (beginIndex > 0)
        return createPathStub(filePath.substring(0, beginIndex));
      else if (beginIndex < 0 || this.equals(getRoot()))
        return null;
      else
        return getRoot();
    }

    @Override
    public Path getRoot() {
      return createPathStub(SEPARATOR);
    }

    @Override
    public boolean isAbsolute() {
      return filePath.startsWith(SEPARATOR);
    }

    @Override
    public @Nonnull Path toAbsolutePath() {
      return isAbsolute() ? this : getRoot().resolve(this);
    }

    @Override
    public boolean endsWith(@Nonnull Path other) {
      return filePath.endsWith(SEPARATOR + other);
    }

    @Override
    public int getNameCount() {
      return filePath.split(SEPARATOR).length;
    }

    @Override
    public @Nonnull Path getName(int index) {
      return createPathStub(filePath.split(SEPARATOR)[index]);
    }

    @Override
    public @Nonnull Path resolve(@Nonnull String other) {
      String newPath = filePath;
      if (needsFileSeparator(other, newPath)) newPath += SEPARATOR;
      return createPathStub(newPath + other);
    }

    private static boolean needsFileSeparator(String other, String newPath) {
      return !newPath.endsWith(SEPARATOR) && !other.startsWith(SEPARATOR);
    }

    @Override
    public @Nonnull Path resolve(@Nonnull Path other) {
      return resolve(((PathStub) other).filePath);
    }

    @Override
    public @Nonnull Path relativize(Path other) {
      if (other.getClass() != getClass())
        throw new RuntimeException("Attempted to use other Path type");

      final String otherPath = ((PathStub) other).filePath;
      if (otherPath.startsWith(filePath)) {
        String substring = otherPath.substring(filePath.length());
        if (substring.startsWith(SEPARATOR)) substring = substring.substring(1);
        return createPathStub(substring);
      }

      throw new RuntimeException("Only relative paths supported");
    }

    @Override
    public @Nonnull String toString() {
      return filePath;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PathStub paths)) return false;
      return Objects.equals(filePath, paths.filePath);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(filePath);
    }
  }

  abstract static class FileSystemProviderStub extends FileSystemProvider {
    private String throwExceptionOnAccess = null;

    private final Map<String, String> fileContents = new HashMap<>();

    static String getFilePath(Path path) {
      if (!(path instanceof PathStub)) {
        throw new IllegalArgumentException(path.getClass() + " not supported");
      }

      return ((PathStub) path).filePath;
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
      if (null == getPathType(getFilePath(path))) {
        throw new NoSuchFileException("File " + getFilePath(path) + " does not exist");
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends BasicFileAttributes> A readAttributes(
        Path path, Class<A> type, LinkOption... options) {
      if (!type.equals(BasicFileAttributes.class)) {
        throw new IllegalArgumentException("attributes type " + type + " not supported");
      }
      return (A) createAttributes(getFilePath(path));
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
      verifyParentDirectory(dir);
      fileContents.put(getFilePath(dir), DIRECTORY_OBJECT);
    }

    @SuppressWarnings("unchecked")
    @Override
    public DirectoryStream<Path> newDirectoryStream(
        Path dir, DirectoryStream.Filter<? super Path> filter) {
      return createStrictStub(DirectoryStreamStub.class, this, getFilePath(dir));
    }

    @Override
    public SeekableByteChannel newByteChannel(
        Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
      if (path.toString().equals(throwExceptionOnAccess)) throw new IOException("for unit test");

      if (!options.contains(StandardOpenOption.WRITE)) {
        return Optional.ofNullable(fileContents.get(getFilePath(path)))
              .map(s -> createStrictStub(ReadOnlyByteChannelStub.class, s))
              .orElseThrow(() -> new FileNotFoundException(path.toString()));
      } else {
        verifyParentDirectory(path);
        return createStrictStub(WriteableByteChannelStub.class, getFilePath(path));
      }
    }

    private void verifyParentDirectory(Path path) throws IOException {
      if (getPathType(getFilePath(path.getParent())) != PathType.DIRECTORY)
        throw new IOException("Parent directory does not exist. May not create " + path);
    }

    private BasicFileAttributes createAttributes(String filePath) {
      return createStrictStub(BasicFileAttributesStub.class, getPathType(filePath));
    }

    private PathType getPathType(String filePath) {
      return PathType.getPathTypeFor(fileContents.get(filePath));
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
      if (source.toString().equals(throwExceptionOnAccess) || target.toString().equals(throwExceptionOnAccess))
        throw new IOException("for unit test");

      if (options.length != 1 && !StandardCopyOption.REPLACE_EXISTING.equals(options[0])) {
        throw new IOException("Only REPLACE_EXISTING option is supported");
      }

      fileContents.put(getFilePath(target), fileContents.get(getFilePath(source)));
    }
  }

  abstract static class BasicFileAttributesStub implements BasicFileAttributes {
    private final PathType pathType;

    BasicFileAttributesStub(PathType pathType) {
      this.pathType = pathType;
    }

    @Override
    public boolean isDirectory() {
      return pathType.isDirectory();
    }

    @Override
    public boolean isRegularFile() {
      return pathType.isRegularFile();
    }

    @Override
    public Object fileKey() {
      return null;
    }
  }

  abstract static class DirectoryStreamStub<T> implements DirectoryStream<T> {
    final List<Path> paths = new ArrayList<>();

    public DirectoryStreamStub(FileSystemProviderStub parent, String root) {
      for (Map.Entry<String,String> entry : parent.fileContents.entrySet()) {
        if (!entry.getValue().equals(DIRECTORY_OBJECT) && entry.getKey().startsWith(root + SEPARATOR)) {
          paths.add(PathStub.createPathStub(entry.getKey()));
        }
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nonnull
    public Iterator<T> iterator() {
      return (Iterator<T>) paths.iterator();
    }

    @Override
    public void close() {
    }
  }

  abstract static class ReadOnlyByteChannelStub implements SeekableByteChannel {
    private final byte[] contents;
    private int index = 0;

    ReadOnlyByteChannelStub(String contents) {
      this.contents = Optional.ofNullable(contents).map(String::getBytes).orElse(null);
    }

    @Override
    public long size() {
      return contents.length;
    }

    @Override
    public long position() {
      return index;
    }

    @Override
    public int read(ByteBuffer dst) {
      if (index >= contents.length) {
        return -1;
      }

      dst.put(contents);
      index = contents.length;
      return contents.length;
    }

    @Override
    public void close() {
    }
  }

  abstract static class WriteableByteChannelStub implements SeekableByteChannel {
    private final String filePath;
    private byte[] contents = new byte[0];

    WriteableByteChannelStub(String filePath) {
      this.filePath = filePath;
    }

    @Override
    public int write(ByteBuffer src) {
      byte[] newContents = new byte[contents.length + src.limit()];
      System.arraycopy(contents, 0, newContents, 0, contents.length);
      System.arraycopy(src.array(), src.position(), newContents, contents.length, src.limit() - src.position());
      contents = newContents;
      src.position(src.limit());
      return contents.length;
    }

    @Override
    public void close() {
      instance.defineFileContents(filePath, new String(contents));
    }
  }
}
