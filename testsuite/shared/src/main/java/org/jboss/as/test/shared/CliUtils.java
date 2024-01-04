/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared;

import java.io.File;
import java.util.Objects;

/**
 * CLI helper methods.
 *
 * @author Josef Cacek
 */
public class CliUtils {

    /**
     * Escapes given path String for CLI.
     *
     * @param path path string to escape (must be not-<code>null</code>)
     * @return escaped path
     */
    public static String escapePath(String path) {
        return Objects.requireNonNull(path, "Path to escape can't be null.").replace("\\", "\\\\");
    }

    /**
     * Returns escaped absolute path of given File instance.
     *
     * @param file instance to get the path from (must be not-<code>null</code>)
     * @return escaped absolute path
     */
    public static String asAbsolutePath(File file) {
        return escapePath(Objects.requireNonNull(file, "File can't be null.").getAbsolutePath());
    }
}
