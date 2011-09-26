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

package org.jboss.as.process.protocol;

import java.io.IOException;
import java.io.OutputStream;
import org.jboss.marshalling.ByteOutput;

/**
 * Byte output implementation that writes the bytes out in chunks.  Each invocation of flush will first write a
 * {@code CHUNK_START} header followed by the size of the chunk being flushed.  Once the closed, this will write out a
 * {@code END} byte.  This should be used to write data that can be read by a {@link ChunkyByteInput}
 * to ensure the reader can not read more than available.  This is handy if the consumer of the stream is prone to over-buffering.
 *
 * @author John Bailey
 */
public class ChunkyByteOutput extends OutputStream implements ByteOutput {
    public static final int CHUNK_START = 0x98;
    public static final int END = 0x99;
    private final ByteOutput output;
    private final byte[] buffer;
    private int position;

    public ChunkyByteOutput(final ByteOutput output) {
        this(output, 8192);
    }

    public ChunkyByteOutput(final ByteOutput output, final int bufferSize) {
        this.output = output;
        buffer = new byte[bufferSize];
    }

    /** {@inheritDoc} */
    public void write(int v) throws IOException {
        final byte[] buffer = this.buffer;
        final int position = this.position;
        if (position == buffer.length) {
            flush();
            buffer[0] = (byte) v;
            this.position = 1;
        } else {
            buffer[position] = (byte) v;
            this.position = position + 1;
        }
    }

   /** {@inheritDoc} */
    public void write(final byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

    /** {@inheritDoc} */
    public void write(final byte[] bytes, final int off, int len) throws IOException {
        byte[] buffer = this.buffer;
        final int position = this.position;
        int offSet = off;
        while (offSet < len) {
            int remaining = buffer.length - position;
            int toRead = len - offSet;
            if (toRead < remaining) {
                System.arraycopy(bytes, offSet, buffer, position, toRead);
                this.position = position + toRead;
                offSet = offSet + len;
            } else {
                System.arraycopy(bytes, offSet, buffer, position, remaining);
                this.position += remaining;
                flush();
                offSet = offSet + remaining;
            }
        }
    }

    /**
     * Flushes the current buffer then write a {@code END} byte.  <em>This will not close the underlying byte output</em>
     *
     * @throws IOException
     */
    public void close() throws IOException {
        flush();
        output.write(END); // Don't close the underlying output
    }

    /**
     * Writes a {@code CHUNK_START} header followed by the size of chunk being flushed followed by the data being flushed.
     * It will then flush the underlying byte output.
     *
     * @throws IOException
     */
    public void flush() throws IOException {
        final ByteOutput output = this.output;
        final int pos = this.position;
        if (pos > 0) {
            output.write(CHUNK_START);
            writeInt(pos);
            final byte[] buffer = this.buffer;
            output.write(buffer, 0, pos);
        }
        this.position = 0;
    }

    public void writeInt(final int i) throws IOException {
        final ByteOutput output = this.output;
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (i >> 24);
        bytes[1] = (byte) (i >> 16);
        bytes[2] = (byte) (i >> 8);
        bytes[3] = (byte) i;
        output.write(bytes);
    }
}
