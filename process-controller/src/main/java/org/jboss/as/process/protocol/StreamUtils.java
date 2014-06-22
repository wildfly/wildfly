/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.process.protocol;

import org.jboss.as.process.logging.ProcessLogger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;

import javax.xml.stream.XMLStreamWriter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class StreamUtils {

    private StreamUtils() {
    }

    public static int readChar(final InputStream input) throws IOException {
        final int a = input.read();
        //System.err.println((char)a + "(" + a + ")");
        if (a < 0) {
            return -1;
        } else if (a == 0) {
            return -1;
        } else if (a < 0x80) {
            return (char)a;
        } else if (a < 0xc0) {
            throw ProcessLogger.ROOT_LOGGER.invalidByte();
        } else if (a < 0xe0) {
            final int b = input.read();
            if (b == -1) {
                throw new EOFException();
            } else if ((b & 0xc0) != 0x80) {
                throw ProcessLogger.ROOT_LOGGER.invalidByte((char)a, a);
            }
            return (a & 0x1f) << 6 | b & 0x3f;
        } else if (a < 0xf0) {
            final int b = input.read();
            if (b == -1) {
                throw new EOFException();
            } else if ((b & 0xc0) != 0x80) {
            throw ProcessLogger.ROOT_LOGGER.invalidByte();
            }
            final int c = input.read();
            if (c == -1) {
                throw new EOFException();
            } else if ((c & 0xc0) != 0x80) {
            throw ProcessLogger.ROOT_LOGGER.invalidByte();
            }
            return (a & 0x0f) << 12 | (b & 0x3f) << 6 | c & 0x3f;
        } else {
            throw ProcessLogger.ROOT_LOGGER.invalidByte();
        }
    }

    public static void readToEol(final InputStream input) throws IOException {
        for (;;) {
            switch (input.read()) {
                case -1: return;
                case '\n': return;
            }
        }
    }

    public static byte[] readBytesWithLength(final InputStream in) throws IOException {
        int expectedLength = readInt(in);
        byte[] bytes = new byte[expectedLength];
        readFully(in, bytes, 0, expectedLength);
        return bytes;
    }

    public static boolean readBoolean(final InputStream input) throws IOException {
        return readUnsignedByte(input) != 0;
    }

    public static int readUnsignedShort(final InputStream input) throws IOException {
        int ch1 = input.read();
        int ch2 = input.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return ((ch1 << 8) + (ch2));
    }

    public static int readInt(final InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    public static void readFully(final InputStream in, byte[] b) throws IOException {
        readFully(in, b, 0, b.length);
    }

    public static void readFully(final InputStream in, byte[] b, int off, int len) throws IOException {
        if (len < 0)
            throw new IndexOutOfBoundsException();
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count < 0)
                throw ProcessLogger.ROOT_LOGGER.readBytes(n);
            n += count;
        }
    }

    public static long readLong(final InputStream in) throws IOException {
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

    public static void writeString(final OutputStream output, final Object o) throws IOException {
        writeString(output, o.toString());
    }

    public static void writeString(final OutputStream output, final String s) throws IOException {
        final int length = s.length();

        int strIdx = 0;
        while (strIdx < length) {
            writeChar(output, s.charAt(strIdx ++));
        }
    }

    public static void writeChar(final OutputStream output, final char c) throws IOException {

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


    public static void writeShort(final OutputStream out, final int value) throws IOException {
        out.write(value >>> 8);
        out.write(value);
    }

    public static void writeInt(final OutputStream out, final int v) throws IOException {
        out.write((v >>> 24) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>>  8) & 0xFF);
        out.write((v >>>  0) & 0xFF);
    }

    public static void writeLong(final OutputStream out, final long v) throws IOException {
        out.write((byte) (v >>> 56) & 0xFF);
        out.write((byte) (v >>> 48) & 0xFF);
        out.write((byte) (v >>> 40) & 0xFF);
        out.write((byte) (v >>> 32) & 0xFF);
        out.write((byte) (v >>> 24) & 0xFF);
        out.write((byte) (v >>> 16) & 0xFF);
        out.write((byte) (v >>>  8) & 0xFF);
        out.write((byte) (v >>>  0) & 0xFF);
    }

    public static void writeBoolean(final OutputStream os, final boolean b) throws IOException {
        os.write(b ? 1 : 0);
    }

    public static byte readByte(final InputStream stream) throws IOException {
        int b = stream.read();
        if (b == -1) {
            throw new EOFException();
        }
        return (byte) b;
    }

    public static int readUnsignedByte(final InputStream stream) throws IOException {
        int b = stream.read();
        if (b == -1) {
            throw new EOFException();
        }
        return b;
    }

    public static void copyStream(final InputStream in, final OutputStream out) throws IOException {
        final byte[] bytes = new byte[8192];
        int cnt;
        while ((cnt = in.read(bytes)) != -1) {
            out.write(bytes, 0, cnt);
        }
    }

    public static String readUTFZBytes(final InputStream input) throws IOException {
        final StringBuilder builder = new StringBuilder();
        for (;;) {
            final int c = readUTFChar(input);
            if (c == -1) {
                return builder.toString();
            }
            builder.append((char) c);
        }
    }

    private static int readUTFChar(final InputStream input) throws IOException {
        final int a = input.read();
        if (a < 0) {
            throw new EOFException();
        } else if (a == 0) {
            return -1;
        } else if (a < 0x80) {
            return (char)a;
        } else if (a < 0xc0) {
            throw ProcessLogger.ROOT_LOGGER.invalidByte();
        } else if (a < 0xe0) {
            final int b = input.read();
            if (b == -1) {
                throw new EOFException();
            } else if ((b & 0xc0) != 0x80) {
            throw ProcessLogger.ROOT_LOGGER.invalidByte();
            }
            return (a & 0x1f) << 6 | b & 0x3f;
        } else if (a < 0xf0) {
            final int b = input.read();
            if (b == -1) {
                throw new EOFException();
            } else if ((b & 0xc0) != 0x80) {
            throw ProcessLogger.ROOT_LOGGER.invalidByte();
            }
            final int c = input.read();
            if (c == -1) {
                throw new EOFException();
            } else if ((c & 0xc0) != 0x80) {
            throw ProcessLogger.ROOT_LOGGER.invalidByte();
            }
            return (a & 0x0f) << 12 | (b & 0x3f) << 6 | c & 0x3f;
        } else {
            throw ProcessLogger.ROOT_LOGGER.invalidByte();
        }
    }

    public static void writeUTFZBytes(final OutputStream outputStream, String string) throws IOException {
        final int len = string.length();
        for (int i = 0; i < len; i ++) {
            writeChar(outputStream, string.charAt(i));
        }
        outputStream.write(0);
    }

    public static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable t) {
            ProcessLogger.PROTOCOL_LOGGER.failedToCloseResource(t, closeable);
        }
    }

    public static void safeClose(final Socket socket) {
        if (socket != null) try {
            socket.close();
        } catch (Throwable t) {
            ProcessLogger.PROTOCOL_LOGGER.failedToCloseResource(t, socket);
        }
    }

    public static void safeClose(final ServerSocket serverSocket) {
        if (serverSocket != null) try {
            serverSocket.close();
        } catch (IOException e) {
            ProcessLogger.PROTOCOL_LOGGER.failedToCloseServerSocket(e, serverSocket);
        }
    }

    public static void safeFinish(final Marshaller marshaller) {
        if (marshaller != null) try {
            marshaller.finish();
        } catch (IOException e) {
            ProcessLogger.PROTOCOL_LOGGER.failedToFinishMarshaller(e, marshaller);
        }
    }

    public static void safeFinish(final Unmarshaller unmarshaller) {
        if (unmarshaller != null) try {
            unmarshaller.finish();
        } catch (IOException e) {
            ProcessLogger.PROTOCOL_LOGGER.failedToFinishUnmarshaller(e, unmarshaller);
        }
    }

    public static void safeClose(final XMLStreamWriter writer) {
        if (writer != null) try {
            writer.close();
        } catch (Throwable t) {
            ProcessLogger.PROTOCOL_LOGGER.failedToCloseResource(t, writer);
        }
    }
}
