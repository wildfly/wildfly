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

package org.jboss.as.patching.runner;

import org.jboss.as.protocol.StreamUtils;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
public final class PatchUtils {

    private static final MessageDigest DIGEST;
    static {
        try {
            DIGEST = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    static final int DEFAULT_BUFFER_SIZE = 65536;
    public static final OutputStream NULL_OUTPUT_STREAM = new OutputStream() {
        public void write(int b) throws IOException {
            //
        }
    };

    public static String readRef(final File file) throws IOException {
        final InputStream is = new FileInputStream(file);
        try {
            return readRef(is);
        } finally {
            safeClose(is);
        }
    }

    public static List<String> readRefs(final File file) throws IOException {
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

    public static void writeRef(final File file, final String ref) throws IOException {
        final OutputStream os = new FileOutputStream(file);
        try {
            writeLine(os, ref);
            os.flush();
            os.close();
        } finally {
            safeClose(os);
        }
    }

    public static void writeRefs(final File file, final List<String> refs) throws IOException {
        final OutputStream os = new FileOutputStream(file);
        try {
            writeRefs(os, refs);
            os.flush();
            os.close();
        } finally {
            safeClose(os);
        }
    }

    static void writeRefs(final OutputStream os, final List<String> refs) throws IOException {
        for(final String ref : refs) {
            writeLine(os, ref);
        }
    }

    static void writeLine(final OutputStream os, final String s) throws IOException {
        os.write(s.getBytes());
        os.write('\n');
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

    static void createDirIfNotExists(final File dir) throws IOException {
        if(! dir.exists()) {
            if(!dir.mkdir() && !dir.exists()) {
                throw new IOException("failed to create " + dir.getAbsolutePath());
            }
        }
    }

    /**
     * Safely close some resource without throwing an exception.
     *
     * @param closeable the resource to close
     */
    public static void safeClose(final Closeable closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                //
            }
        }
    }

    /**
     * Copy a file.
     *
     * @param in the
     * @param out the output
     *
     * @throws IOException for any error
     */
    public static void copyFile(final File in, final File out) throws IOException {
        final InputStream is = new FileInputStream(in);
        try {
            final OutputStream os = new FileOutputStream(out);
            try {
                copyStreamAndClose(is, os);
            } finally {
                safeClose(os);
            }
        } finally {
            safeClose(is);
        }
    }

    /**
     * Copy input stream to output stream and close them both
     *
     * @param is input stream
     * @param os output stream
     *
     * @throws IOException for any error
     */
    public static void copyStreamAndClose(InputStream is, OutputStream os) throws IOException {
        copyStreamAndClose(is, os, DEFAULT_BUFFER_SIZE);
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
    public static void copyStreamAndClose(InputStream is, OutputStream os, int bufferSize)
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
    public static void copyStream(InputStream is, OutputStream os, int bufferSize)
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

    public static byte[] copyAndGetHash(final InputStream is, final OutputStream os) throws IOException {
        byte[] sha1Bytes;
        synchronized (DIGEST) {
            DIGEST.reset();
            BufferedInputStream bis = new BufferedInputStream(is);
            DigestOutputStream dos = new DigestOutputStream(os, DIGEST);
            copyStream(bis, dos);
            sha1Bytes = DIGEST.digest();
        }
        return sha1Bytes;
    }

    /**
     * Calculate the hash of a file.
     *
     * @param file the file
     * @return the hash
     * @throws IOException
     */
    public static byte[] calculateHash(final File file) throws IOException {
        final InputStream is = new FileInputStream(file);
        try {
            final DigestOutputStream os = new DigestOutputStream(PatchUtils.NULL_OUTPUT_STREAM, DIGEST);
            synchronized (DIGEST) {
                DIGEST.reset();
                PatchUtils.copyStream(is, os);
                return DIGEST.digest();
            }
        } finally {
            StreamUtils.safeClose(is);
        }
    }

    private static char[] table = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

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

}