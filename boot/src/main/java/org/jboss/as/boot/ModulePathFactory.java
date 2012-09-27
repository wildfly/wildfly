/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.boot;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
class ModulePathFactory {

    static File[] resolveModulePath(final File[] modulePath) {
        final String jbossHome = System.getProperty("jboss.home", System.getenv("JBOSS_HOME"));
        final File root = jbossHome == null ? new File(".") : new File(jbossHome); // TODO
        try {
            return load(root, modulePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static File[] load(final File jbossHome, final File[] modulePath) throws IOException {
        final DirectoryStructure structure = DirectoryStructure.createDefault(jbossHome);
        if(structure.getCumulativeLink().exists()) {
            final String ref = readRef(structure.getCumulativeLink());
            final List<String> patches = readRefs(structure.getCumulativeRefs(ref));
            final List<File> path = new ArrayList<File>(patches.size() + modulePath.length + 1);
            for(final String patch : patches) {
                // one-off patches
                path.add(structure.getModulePatchDirectory(patch));
            }
            // CP
            path.add(structure.getModulePatchDirectory(ref));
            // User defined module path
            for(final File file : modulePath) {
                path.add(file);
            }
            return path.toArray(new File[path.size()]);
        } else {
            return modulePath;
        }
    }

    static String readRef(final File file) throws IOException {
        final InputStream is = new FileInputStream(file);
        try {
            return readRef(is);
        } finally {
            safeClose(is);
        }
    }

    static List<String> readRefs(final File file) throws IOException {
        if(! file.exists()) {
            return Collections.emptyList();
        }
        final InputStream is = new FileInputStream(file);
        try {
            return readRefs(is);
        } finally {
            safeClose(is);
        }
    }

    static String readRef(final InputStream is) throws IOException {
        final StringBuffer buffer = new StringBuffer();
        readLine(is, buffer);
        return buffer.toString();
    }

    static List<String> readRefs(final InputStream is) throws IOException {
        final List<String> refs = new ArrayList<String>();
        final StringBuffer buffer = new StringBuffer();
        do {
            if(buffer.length() > 0) {
                final String ref = buffer.toString().trim();
                if(ref.length() > 0) {
                    refs.add(ref);
                }
            }
        } while(readLine(is, buffer));
        return refs;
    }

    static boolean readLine(InputStream is, StringBuffer buffer) throws IOException {
        buffer.setLength(0);
        int c;
        for(;;) {
            c = is.read();
            switch(c) {
                case '\t':
                case '\r':
                    break;
                case -1: return false;
                case '\n': return true;
                default: buffer.append((char) c);
            }
        }
    }

    static void safeClose(final Closeable closeable) {
        if(closeable != null) try {
            closeable.close();
        } catch (IOException e) {
            //
        }
    }
}
