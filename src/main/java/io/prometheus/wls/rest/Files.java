package io.prometheus.wls.rest;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class Files {

    private static FileSystem fileSystem = new LiveFileSystem();

    public static InputStream openFile(String fileName) throws FileNotFoundException {
        return fileSystem.openFileFromClasspath(fileName);
    }

    public static class LiveFileSystem implements FileSystem {
        @Override
        public InputStream openFileFromClasspath(String fileName) throws FileNotFoundException {
            return Files.class.getResourceAsStream(fileName);
        }
    }
}
