/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.domain.mixed.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class OldVersionCopier {

    private static String OLD_VERSIONS_DIR = "jboss.test.mixed.domain.dir";

    private final File oldVersionsBaseDir;
    private final File targetOldVersions = new File("target/old-versions/");


    private OldVersionCopier(File oldVersionsBaseDir) {
        this.oldVersionsBaseDir = oldVersionsBaseDir;
    }

    static OldVersionCopier expandOldVersions() {
        OldVersionCopier copier = new OldVersionCopier(obtainOldVersionsDir());
        copier.expandAsInstances();
        return copier;
    }

    File getVersionDir(String version) {
        File file = new File(targetOldVersions, "jboss-as-" + version);
        if (!file.exists() || !file.isDirectory()) {
            throw new IllegalStateException("Could not find " + file.getAbsolutePath());
        }
        return file;
    }

    private static File obtainOldVersionsDir() {
        String error = "System property '" + OLD_VERSIONS_DIR + "' must be set to a directory containing old versions";
        String oldVersionsDir = System.getProperty(OLD_VERSIONS_DIR);
        if (oldVersionsDir == null) {
            throw new RuntimeException(error);
        }
        File file = new File(oldVersionsDir);
        if (!file.exists() || !file.isDirectory()) {
            throw new RuntimeException(error);
        }
        return file;
    }

    private void expandAsInstances() {
        if (targetOldVersions.exists()) {
            return;
        }
        if (!targetOldVersions.mkdirs() && targetOldVersions.exists()) {
            throw new RuntimeException("Could not create " + targetOldVersions);
        }

        for (File file : oldVersionsBaseDir.listFiles()) {
            if (file.getName().endsWith(".zip")) {
                try {
                    expandAsInstance(file);
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void expandAsInstance(final File file) throws Exception {

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
            for (Enumeration<? extends ZipEntry> en = zipFile.entries() ; en.hasMoreElements() ; ) {
                final ZipEntry entry = en.nextElement();
                final File output = new File(targetOldVersions, entry.getName());
                if (entry.isDirectory()) {
                    if (!output.exists()) {
                        if (!output.mkdirs() && !output.exists()) {
                            throw new RuntimeException("Could not make dir " + output.getAbsolutePath());
                        }
                    }
                } else {
                    final InputStream in = new BufferedInputStream(zipFile.getInputStream(entry));
                    try {
                        final OutputStream out = new BufferedOutputStream(new FileOutputStream(output));
                        try {
                            int i = in.read();
                            while (i != -1) {
                                out.write((byte)i);
                                i = in.read();
                            }
                        } finally {
                            IoUtils.safeClose(out);
                        }
                    } finally {
                        IoUtils.safeClose(in);
                    }
                }

            }
        } finally {
            IoUtils.safeClose(zipFile);
        }
    }

    public static void main(String[] args) {
        System.setProperty(OLD_VERSIONS_DIR, "/Users/kabir/old-as7-releases/");
        OldVersionCopier.expandOldVersions();
    }
}
