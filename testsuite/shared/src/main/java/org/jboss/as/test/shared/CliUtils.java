/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
