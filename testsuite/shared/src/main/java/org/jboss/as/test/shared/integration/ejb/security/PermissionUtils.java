/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.shared.integration.ejb.security;

import java.io.File;
import java.io.FilePermission;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PermissionUtils {

    /**
     * Creates a new {@link FilePermission} with the base path of the system property {@code jboss.inst}.
     *
     * @param action the actions required
     * @param paths  the relative parts of the path
     *
     * @return the new file permission
     *
     * @see FilePermission
     * @see #createFilePermission(String, String, Iterable)
     */
    public static FilePermission createFilePermission(final String action, final String... paths) {
        return createFilePermission(action, "jboss.inst", Arrays.asList(paths));
    }

    /**
     * Creates a new {@link FilePermission}.
     * <p>
     * The paths are iterated with a {@link File#separatorChar} be placed after each path portion. The
     * {@code sysPropKey} is used to resolve the base directory which the {@code paths} will be appended to.
     * </p>
     * <p>
     * The base path is validated and must exist as well as be a directory. The path is converted to an
     * {@linkplain Path#toAbsolutePath() absolute} path as well as {@linkplain Path#normalize() normalized}.
     * </p>
     * <pre>
     * {@code
     * // The following produces the absolute path of target/wildfly/standalone/tmp/example/*
     * createFilePermission("read", "jboss.inst", Arrays.asList("standalone", "tmp", "example", "*"));
     *
     * // The following produces the absolute path of target/wildfly/standalone/data/-
     * createFilePermission("read", "jboss.inst", Arrays.asList("standalone", "data", "-"));
     *
     * // The following produces the absolute path of target/wildfly/standalone/data/example
     * createFilePermission("read", "jboss.inst", Arrays.asList("standalone", "data", "example"));
     * }
     * </pre>
     *
     * @param action     the actions required
     * @param sysPropKey the system property key to resolve the base directory
     * @param paths      the relative parts of the path to be appended to the base directory
     *
     * @return the new file permission
     *
     * @see FilePermission
     */
    public static FilePermission createFilePermission(final String action, final String sysPropKey, final Iterable<String> paths) {
        final String prop = System.getProperty(sysPropKey);
        if (prop == null) {
            throw new IllegalArgumentException(String.format("Could not find the system property %s", sysPropKey));
        }
        final Path base = Paths.get(prop);
        if (Files.notExists(base)) {
            throw new RuntimeException(String.format("The system property %s resolved to %s which does not exist.", sysPropKey, base));
        }
        if (!Files.isDirectory(base)) {
            throw new RuntimeException(String.format("The system property %s resolved to %s which is not a directory.", sysPropKey, base));
        }
        final StringBuilder path = new StringBuilder(256)
                .append(base.toAbsolutePath().normalize())
                .append(File.separatorChar);
        final Iterator<String> iter = paths.iterator();
        while (iter.hasNext()) {
            path.append(iter.next());
            if (iter.hasNext()) {
                path.append(File.separatorChar);
            }
        }
        return new FilePermission(path.toString(), action);
    }
}
