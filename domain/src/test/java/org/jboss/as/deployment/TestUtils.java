/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.deployment;

import org.jboss.vfs.VFSUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;

/**
 * @author John E. Bailey
 */
public class TestUtils {
    private TestUtils() {
    }

    public static URL getResource(final Class<?> testClass, final String path) throws Exception {
        return testClass.getResource(path);
    }

    public static File getResourceFile(final Class<?> testClass, final String path) throws Exception {
        return new File(getResource(testClass, path).toURI());
    }

    public static void copyResource(final Class<?> testClass, final String inputResource, final String outputBase, final String outputPath) throws Exception {
        final File resource = getResourceFile(testClass, inputResource);
        final File outputDirectory = new File(getResourceFile(testClass, outputBase), outputPath);

        if (!resource.exists())
            throw new IllegalArgumentException("Resource does not exist");
        if (outputDirectory.exists() && outputDirectory.isFile())
            throw new IllegalArgumentException("OutputDirectory must be a directory");
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs())
                throw new RuntimeException("Failed to create output directory");
        }
        final File outputFile = new File(outputDirectory, resource.getName());
        final InputStream in = new FileInputStream(resource);
        try {
            final OutputStream out = new FileOutputStream(outputFile);
            try {
                final byte[] b = new byte[8192];
                int c;
                while ((c = in.read(b)) != -1) {
                    out.write(b, 0, c);
                }
                out.close();
                in.close();
            } finally {
                VFSUtils.safeClose(out);
            }
        } finally {
            VFSUtils.safeClose(in);
        }
    }

    public static <T> T getPrivateFieldValue(final Object target, final String fieldName, final Class<T> fieldType) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }
}
