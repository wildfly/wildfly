/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.process;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class StreamUtils {

    private StreamUtils() {
    }

    static Status readWord(final InputStream input, final StringBuilder dest) throws IOException {
        dest.setLength(0);
        int c;
        for (;;) {
            c = readChar(input);
            switch (c) {
                case -1: return Status.END_OF_STREAM;
                case 0: return Status.MORE;
                case '\n': return Status.END_OF_LINE;
                default: dest.append((char) c);
            }
        }
    }

    private static final String INVALID_BYTE = "Invalid byte";

    static int readChar(final InputStream input) throws IOException {
        final int a = input.read();
        if (a < 0) {
            throw new EOFException();
        } else if (a == 0) {
            return -1;
        } else if (a < 0x80) {
            return (char)a;
        } else if (a < 0xc0) {
            throw new UTFDataFormatException(INVALID_BYTE);
        } else if (a < 0xe0) {
            final int b = input.read();
            if (b == -1) {
                throw new EOFException();
            } else if ((b & 0xc0) != 0x80) {
                throw new UTFDataFormatException(INVALID_BYTE);
            }
            return (a & 0x1f) << 6 | b & 0x3f;
        } else if (a < 0xf0) {
            final int b = input.read();
            if (b == -1) {
                throw new EOFException();
            } else if ((b & 0xc0) != 0x80) {
                throw new UTFDataFormatException(INVALID_BYTE);
            }
            final int c = input.read();
            if (c == -1) {
                throw new EOFException();
            } else if ((c & 0xc0) != 0x80) {
                throw new UTFDataFormatException(INVALID_BYTE);
            }
            return (a & 0x0f) << 12 | (b & 0x3f) << 6 | c & 0x3f;
        } else {
            throw new UTFDataFormatException(INVALID_BYTE);
        }
    }

    static void readToEol(final InputStream input) throws IOException {
        for (;;) {
            switch (input.read()) {
                case -1: return;
                case '\n': return;
            }
        }
    }

    static void writeString(final OutputStream output, final Object o) throws IOException {
        writeString(output, o.toString());
    }

    static void writeString(final OutputStream output, final String s) throws IOException {
        final int length = s.length();

        int strIdx = 0;
        while (strIdx < length) {
            final char c = s.charAt(strIdx ++);
            if (c >= 0x20 && c <= 0x7f) {
                output.write((byte)c);
            } else if (c <= 0x07ff) {
                output.write((byte)(0xc0 | 0x1f & c >> 6));
                output.write((byte)(0x80 | 0x3f & c));
            } else {
                output.write((byte)(0xe0 | 0x0f & c >> 12));
                output.write((byte)(0x80 | 0x3f & c >> 6));
                output.write((byte)(0x80 | 0x3f & c));
            }
        }
    }
}
