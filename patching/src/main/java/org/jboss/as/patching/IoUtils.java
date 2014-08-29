/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipFile;

/**
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 * @author <a href="http://jmesnil/net/">Jeff Mesnil</a> (c) 2012 Red Hat Inc
 */
public class IoUtils {

    public static byte[] NO_CONTENT = new byte[0];

    private static final int DEFAULT_BUFFER_SIZE = 65536;

    /**
     * Copy input stream to output stream and close them both
     *
     * @param is input stream
     * @param os output stream
     *
     * @throws IOException for any error
     */
    public static void copyStreamAndClose(InputStream is, OutputStream os) throws IOException {
        try {
            copyStream(is, os, DEFAULT_BUFFER_SIZE);
            // throw an exception if the close fails since some data might be lost
            is.close();
            os.close();
        }
        finally {
            // ...but still guarantee that they're both closed
            safeClose(is);
            safeClose(os);
        }
    }

    /**
     * Copy input stream to output stream without closing streams. Flushes output stream when done.
     *
     * @param is input stream
     * @param os output stream
     *
     * @throws IOException for any error
     */
    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        copyStream(is, os, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Copy input stream to output stream without closing streams. Flushes output stream when done.
     *
     * @param is input stream
     * @param os output stream
     * @param bufferSize the buffer size to use
     *
     * @throws IOException for any error
     */
    private static void copyStream(InputStream is, OutputStream os, int bufferSize)
            throws IOException {
        if (is == null) {
            throw PatchMessages.MESSAGES.nullInputStream();
        }
        if (os == null) {
            throw PatchMessages.MESSAGES.nullOutputStream();
        }
        byte[] buff = new byte[bufferSize];
        int rc;
        while ((rc = is.read(buff)) != -1) os.write(buff, 0, rc);
        os.flush();
    }

    public static void copyFile(File sourceFile, File targetFile) throws IOException {
        if (sourceFile.isDirectory()) {
            copyDir(sourceFile, targetFile);
        } else {
            File parent = targetFile.getParentFile();
            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    throw PatchMessages.MESSAGES.cannotCreateDirectory(parent.getAbsolutePath());
                }
            }
            final InputStream is = new FileInputStream(sourceFile);
            final OutputStream os = new FileOutputStream(targetFile);
            copyStreamAndClose(is, os);
        }
    }

    private static void copyDir(File sourceDir, File targetDir) throws IOException {
        if (targetDir.exists()) {
            if (!targetDir.isDirectory()) {
                throw PatchMessages.MESSAGES.notADirectory(targetDir.getAbsolutePath());
            }
        } else if (!targetDir.mkdirs()) {
            throw PatchMessages.MESSAGES.cannotCreateDirectory(targetDir.getAbsolutePath());
        }

        File[] children = sourceDir.listFiles();
        if (children != null) {
            for (File child : children) {
                copyFile(child, new File(targetDir, child.getName()));
            }
        }
    }

    public static byte[] copy(final InputStream is, final File target) throws IOException {
        if(! target.getParentFile().exists()) {
            target.getParentFile().mkdirs(); // Hmm
        }
        final OutputStream os = new FileOutputStream(target);
        try {
            byte[] nh = HashUtils.copyAndGetHash(is, os);
            os.close();
            return nh;
        } finally {
            safeClose(os);
        }
    }

    public static byte[] copy(File source, File target) throws IOException {
        final FileInputStream is = new FileInputStream(source);
        try {
            byte[] backupHash = copy(is, target);
            is.close();
            return backupHash;
        } finally {
            safeClose(is);
        }
    }

    public static void safeClose(final Closeable closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                //
            }
        }
    }

    public static void safeClose(final ZipFile closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                //
            }
        }
    }

    public static boolean recursiveDelete(File root) {
        if (root == null) {
            return true;
        }
        boolean ok = true;
        if (root.isDirectory()) {
            final File[] files = root.listFiles();
            for (File file : files) {
                ok &= recursiveDelete(file);
            }
            return ok && (root.delete() || !root.exists());
        } else {
            ok &= root.delete() || !root.exists();
        }
        return ok;
    }

    public static File mkdir(File parent, String... segments) throws IOException {
        File dir = parent;
        for (String segment : segments) {
            dir = new File(dir, segment);
        }
        dir.mkdirs();
        return dir;
    }

    /**
     * Return a new File object based on the baseDir and the segments.
     *
     * This method does not perform any operation on the file system.
     */
    public static File newFile(File baseDir, String... segments) {
        File f = baseDir;
        for (String segment : segments) {
            f = new File(f, segment);
        }
        return f;
    }

}
