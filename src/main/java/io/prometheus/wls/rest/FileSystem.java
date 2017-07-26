package io.prometheus.wls.rest;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * A wrapper for the file system to enable unit testing.
 */
interface FileSystem {

    /**
     * Open the specified file for reading.
     * @param fileName the path to the file, relative to the classpath.
     * @return a Reader for the file contents.
     * @throws FileNotFoundException if the file name cannot be resolved
     */
    public abstract InputStream openFileFromClasspath(String fileName) throws FileNotFoundException;

}
