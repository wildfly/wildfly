/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.compat.util;

import static java.lang.String.format;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * This factory allows us to build strings representing the persistence.xml files.
 */
public class SystemTestStringUtil {

    private static final String SCANNER_DELIMITER = "\\A";
    public static SystemTestStringUtil SINGLETON = new SystemTestStringUtil();

    private SystemTestStringUtil() {
    }

    /**
     * Trivially read a file into string.
     *
     * @param file file to read
     * @return The corresponding file contents
     */
    public String getFileAsString(File file) {
        if (!file.exists()) {
            throw new RuntimeException(format("The file %1$s does not exist", file));
        }
        try {
            try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
                return convertStreamToStringUntilFirstDelimiter(inputStream);
            }
        } catch (Exception e) {
            String errMsg = String.format("Unexpected error while attempting to read file: %1$s", file);
            throw new RuntimeException(errMsg);
        }

    }

    /**
     * Read an input stream expected to contain UTF-8 text into a string. The caller is responsible for closing the input sream.
     *
     * @param inputStream data to read
     * @return the corresponding string.
     */
    public String convertStreamToStringUntilFirstDelimiter(InputStream inputStream) {
        try (java.util.Scanner scanner = new java.util.Scanner(inputStream, java.nio.charset.StandardCharsets.UTF_8.toString())) {
            // (a) set delimiter that shall not be found on the stream
            scanner.useDelimiter(SCANNER_DELIMITER);
            // (b) read file until EOF
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
}
