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

package org.jboss.as.patching.generator;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utilities related to patch file generation.
 *
 * TODO unify utility files.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class PatchUtils {

    private PatchUtils() {
        // no instantiation
    }

    private static final char[] table = "0123456789abcdef".toCharArray();

    private static final int DEFAULT_BUFFER_SIZE = 65536;

    public static byte[] hashFile(File file, MessageDigest digest) throws IOException {

        synchronized (digest) {
            digest.reset();
            updateDigest(digest, file);
            return digest.digest();
        }
    }

    private static void updateDigest(MessageDigest digest, File file) throws IOException {
        if (file.isDirectory()) {
            File[] childList = file.listFiles();
            if (childList != null) {
                Map<String, File> sortedChildren = new TreeMap<String, File>();
                for (File child : childList) {
                    sortedChildren.put(child.getName(), child);
                }
                for (File child : sortedChildren.values()) {
                    updateDigest(digest, child);
                }
            }
        } else {
            FileInputStream fis = new FileInputStream(file);
            try {

                BufferedInputStream bis = new BufferedInputStream(fis);
                byte[] bytes = new byte[8192];
                int read;
                while ((read = bis.read(bytes)) > -1) {
                    digest.update(bytes, 0, read);
                }
            } finally {
                safeClose(fis);
            }

        }
    }

    /**
     * Convert a byte array into a hex string.
     *
     * @param bytes the bytes
     * @return the string
     */
    public static String bytesToHexString(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(table[b >> 4 & 0x0f]).append(table[b & 0x0f]);
        }
        return builder.toString();
    }

    /**
     * Convert a hex string into a byte[].
     *
     * @param s the string
     * @return the bytes
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len >> 1];
        for (int i = 0, j = 0; j < len; i++) {
            int x = Character.digit(s.charAt(j), 16) << 4;
            j++;
            x = x | Character.digit(s.charAt(j), 16);
            j++;
            data[i] = (byte) (x & 0xFF);
        }
        return data;
    }

    public static void copyFile(File sourceFile, File targetFile) throws IOException {
        if (sourceFile.isDirectory()) {
            copyDir(sourceFile, targetFile);
        } else {
            File parent = targetFile.getParentFile();
            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    throw new IllegalStateException("Cannot create directory " + parent);
                }
            }
            final InputStream is = new FileInputStream(sourceFile);
            try {
                final OutputStream os = new FileOutputStream(targetFile);
                try {
                    copyStreamAndClose(is, os, DEFAULT_BUFFER_SIZE);
                } finally {
                    safeClose(os);
                }
            } finally {
                safeClose(is);
            }
        }
    }

    private static void copyDir(File sourceDir, File targetDir) throws IOException {
        if (targetDir.exists()) {
            if (!targetDir.isDirectory()) {
                throw new IllegalStateException(targetDir + " is not a directory");
            }
        } else if (!targetDir.mkdirs()) {
            throw new IllegalStateException("Cannot create directory " + targetDir);
        }

        File[] children = sourceDir.listFiles();
        if (children != null) {
            for (File child : children) {
                copyFile(child, new File(targetDir, child.getName()));
            }
        }
    }

    /**
     * Copy input stream to output stream and close them both
     *
     * @param is input stream
     * @param os output stream
     * @param bufferSize the buffer size to use
     *
     * @throws IOException for any error
     */
    private static void copyStreamAndClose(InputStream is, OutputStream os, int bufferSize)
            throws IOException {
        try {
            copyStream(is, os, bufferSize);
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

    private static void copyStream(InputStream is, OutputStream os, int bufferSize)
            throws IOException {
        if (is == null) {
            throw new IllegalArgumentException("input stream is null");
        }
        if (os == null) {
            throw new IllegalArgumentException("output stream is null");
        }
        byte[] buff = new byte[bufferSize];
        int rc;
        while ((rc = is.read(buff)) != -1) os.write(buff, 0, rc);
        os.flush();
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
}
