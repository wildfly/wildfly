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
import java.io.InputStream;

import org.jboss.as.process.logging.ProcessLogger;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.Marshalling;

/**
 * Byte input implementation that reads bytes in chunks.  Each chunk is started with a {@code CHUNK_START} header followed
 * by the length of the chunk.  At the end of all the chunks it will run into a {@code END} byte, which will appear as the end
 * of the stream.  This is used when you need to ensure a consumer of the input can not read more than necessary.  This is handy
 * if the consumer of the stream is prone to over-buffering. Note this will only work for byte streams that were written using a
 * {@link ChunkyByteOutput}.
 *
 * @author John Bailey
 */
public class ChunkyByteInput extends InputStream implements ByteInput {
    public static final int CHUNK_START = 0x98;
    public static final int END = 0x99;
    private ByteInput input;
    private int remaining = 0;
    private boolean finished;

    public ChunkyByteInput(final ByteInput byteInput) {
        input = byteInput;
    }

    public ChunkyByteInput(final InputStream inputStream) {
        input = Marshalling.createByteInput(inputStream);
    }

    public ChunkyByteInput(final InputStream inputStream, final int remaining) {
        input = Marshalling.createByteInput(inputStream);
        this.remaining = remaining;
    }

    /**
     * {@inheritDoc}
     */
    public int read() throws IOException {
        if (remaining == 0) {
            startChunk();
        }
        if (remaining < 1) {
            return remaining;
        }
        this.remaining--;
        return input.read();
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * {@inheritDoc}
     */
    public int read(final byte[] b, int off, int len) throws IOException {
        int ret = 0;
        while (len != 0) {
            if (remaining == 0) {
                startChunk();
            }
            if (remaining < 1) {
                return ret;
            }
            int toRead = Math.min(len, remaining);
            int read = input.read(b, off, toRead);
            this.remaining -= read;
            len -= read;
            off += read;
            ret += read;
            if (read < toRead) {
                return ret;
            }
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    public long skip(final long n) throws IOException {
        if (remaining == 0) {
            startChunk();
        }
        if (remaining < 1) {
            return 0;
        }
        final long toSkip = n < remaining ? n : remaining;
        final long ret = input.skip(toSkip);
        this.remaining = remaining - (int) ret;
        return ret;
    }

    public int available() throws IOException {
        return remaining;
    }

    public void close() throws IOException {
        // Don't close the underlying input
        while (!finished) {
            final int current = input.read();
            switch (current) {
                case -1:
                    return;
                case END:
                    finished = true;
                    break;
            }
        }
    }

    private void startChunk() throws IOException {
        final int current = input.read();
        switch (current) {
            case -1:
                remaining = -1;
                break;
            case CHUNK_START:
                remaining = readInt();
                break;
            case END:
                remaining = -1;
                finished = true;
                break;
            default:
                throw ProcessLogger.ROOT_LOGGER.invalidStartChunk(current);
        }
    }

    private int readInt() throws IOException {
        return input.read() << 24 | (input.read() & 0xff) << 16 | (input.read() & 0xff) << 8 | (input.read() & 0xff);
    }
}
