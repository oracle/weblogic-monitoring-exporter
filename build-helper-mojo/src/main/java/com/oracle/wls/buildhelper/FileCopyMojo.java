// Copyright (c) 2020, Oracle and/or its affiliates.
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
import java.util.Optional;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Mojo(name = "copy", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class FileCopyMojo extends AbstractMojo {
    @Parameter(required = true) private String sourceFile;
    @Parameter(required = true) private File targetFile;
    @Parameter private File userDir;

    @SuppressWarnings("FieldMayBeFinal") // leave non-final for unit test
    private static CopyExecutor executor = new CopyExecutorImpl();

    public void execute() throws MojoExecutionException {
        Path source = toPath(sourceFile);
        Path target = toPath(targetFile);
        try {
            getLog().info("Copying " + source + " to " + target);
            Files.createDirectories(target.getParent());
            Files.copy(source, target, REPLACE_EXISTING);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to copy " + source + " to " + target, e);
        }
    }

    private Path toPath(String filePath) {
        return executor.toPath(getEffectiveFile(new File(filePath)));
    }

    private Path toPath(File file) {
        return executor.toPath(getEffectiveFile(file));
    }

    private File getEffectiveFile(File file) {
        return file.isAbsolute() ? file : new File (getUserDir(), file.getPath());
    }

    private File getUserDir() {
        return Optional.ofNullable(userDir).orElse(new File(System.getProperty("user.dir")));
    }
}
