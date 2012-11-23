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

package org.jboss.as.osgi.service;

import org.jboss.as.boot.DirectoryStructure;
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

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
class RepositoryRootUtils {

    static File[] resolveRoots(final ServerEnvironment environment) throws IOException {
        final File jbossHome = environment.getHomeDir();
        final File bundlesDir = environment.getBundlesDir();
        if (bundlesDir.isDirectory() == false) {
            throw MESSAGES.illegalStateArtifactBaseLocation(bundlesDir);
        }
        final File modulesDir = new File(bundlesDir.getParent() + File.separator + "modules");
        if (modulesDir.isDirectory() == false) {
            throw MESSAGES.illegalStateArtifactBaseLocation(modulesDir);
        }
        final DirectoryStructure structure = DirectoryStructure.createDefault(jbossHome);
        if(structure.getCumulativeLink().exists()) {
            final String ref = readRef(structure.getCumulativeLink());
            final List<String> patches = readRefs(structure.getCumulativeRefs(ref));
            final List<File> path = new ArrayList<File>(patches.size() +  1);
            for(final String patch : patches) {
                // one-off patches
                path.add(structure.getBundlesPatchDirectory(patch));
            }
            // CP
            path.add(structure.getBundlesPatchDirectory(ref));
            // User defined module path
            path.add(bundlesDir);
            path.add(modulesDir);
            return path.toArray(new File[path.size()]);
        } else {
            return new File[] { bundlesDir, modulesDir };
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
