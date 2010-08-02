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

package org.jboss.as.process;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.util.zip.Adler32;

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
    
    static CheckedBytes readCheckedBytes(final InputStream input) throws IOException {
        return new CheckedBytes(input);
    }
    
    static int readInt(final InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }
    
    static void readFully(final InputStream in, byte b[], int off, int len) throws IOException {
        if (len < 0)
            throw new IndexOutOfBoundsException();
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count < 0)
            throw new EOFException();
            n += count;
        }
    }
    
    static long readLong(final InputStream in) throws IOException {
        byte[] bytes = new byte[8];
        readFully(in, bytes, 0, 8);
        return (((long)bytes[0] << 56) +
                ((long)(bytes[1] & 255) << 48) +
                ((long)(bytes[2] & 255) << 40) +
                ((long)(bytes[3] & 255) << 32) +
                ((long)(bytes[4] & 255) << 24) +
                ((bytes[5] & 255) << 16) +
                ((bytes[6] & 255) <<  8) +
                ((bytes[7] & 255) <<  0));
    }

    static void writeString(final OutputStream output, final Object o) throws IOException {
        writeString(output, o.toString());
    }

    static void writeString(final OutputStream output, final String s) throws IOException {
        final int length = s.length();

        int strIdx = 0;
        while (strIdx < length) {
            writeChar(output, s.charAt(strIdx ++));
        }
    }
    
    static void writeChar(final OutputStream output, final char c) throws IOException {

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
    
    static void writeLong(final OutputStream out, long v) throws IOException {
        for(int i = 0; i < 8; i++) {
            out.write((byte)((v >> (8 * i)) & 0xff));
          }
    }
    
    public static class CheckedBytes {
         
        private final int expectedLength;
        private byte[] bytez;
        private long checksum;
        private long expectedChecksum;
        
        public int getExpectedLength() {
            return expectedLength;
        }
        public byte[] getBytes() {
            return bytez;
        }
        public long getChecksum() {
            return checksum;
        }
        public long getExpectedChecksum() {
            return expectedChecksum;
        }
        
        private CheckedBytes(InputStream input) throws IOException {
            this.expectedLength = readInt(input);
            this.bytez = new byte[expectedLength];
            readFully(input, bytez, 0, expectedLength);
            this.expectedChecksum = readLong(input);
            Adler32 adler = new Adler32();
            adler.update(bytez, 0, expectedLength);
            this.checksum = adler.getValue();
        }
    }
}
