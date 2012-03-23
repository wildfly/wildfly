/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ee.deployment.spi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import static org.jboss.as.ee.deployment.spi.DeploymentLogger.ROOT_LOGGER;

/**
 * A collection of jar utilities
 *
 * @author Thomas.Diesler@jboss.com
 *
 */
public final class JarUtils {

    /**
     * Add jar contents to the deployment archive under the given prefix
     */
    public static String[] addJar(JarOutputStream outputStream, String prefix, File jar) throws IOException {
        ROOT_LOGGER.tracef("addJar: %s", jar);
        ArrayList tmp = new ArrayList();
        FileInputStream fis = new FileInputStream(jar);
        JarInputStream jis = new JarInputStream(fis);
        JarEntry entry = jis.getNextJarEntry();
        while (entry != null) {
            if (entry.isDirectory() == false) {
                String entryName = prefix + entry.getName();
                tmp.add(entryName);
                addJarEntry(outputStream, entryName, jis);
            }
            entry = jis.getNextJarEntry();
        }
        jis.close();
        String[] names = new String[tmp.size()];
        tmp.toArray(names);
        return names;
    }

    /**
     * Add a jar entry to the deployment archive
     */
    public static void addJarEntry(JarOutputStream outputStream, String entryName, InputStream inputStream) throws IOException {
        ROOT_LOGGER.tracef("addJarEntry: %s", entryName);
        outputStream.putNextEntry(new JarEntry(entryName));
        copyStream(outputStream, inputStream);
    }

    /**
     * Copies the input stream to the output stream
     */
    public static void copyStream(OutputStream outputStream, InputStream inputStream) throws IOException {
        byte[] bytes = new byte[4096];
        int read = inputStream.read(bytes, 0, 4096);
        while (read > 0) {
            outputStream.write(bytes, 0, read);
            read = inputStream.read(bytes, 0, 4096);
        }
    }

}
