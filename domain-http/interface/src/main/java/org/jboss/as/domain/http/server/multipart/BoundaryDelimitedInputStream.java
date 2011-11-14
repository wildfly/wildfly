/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.domain.http.server.multipart;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.jboss.as.domain.http.server.HttpServerLogger.ROOT_LOGGER;

/**
 * <code>BoundaryDelimitedInputStream</code> encapsulates a stream that is separated into different sections by a common
 * boundary, such as a MIME multipart message. The full stream is referred to as the outer stream. Each boundary separated
 * section in the outer stream is referred to as an inner stream. All read() methods will start from the first inner stream,
 * returning -1 when a boundary is reached. Subsequent calls will then advance to the next stream, skipping the boundary.
 *
 * @author Jason T. Greene
 */
public final class BoundaryDelimitedInputStream extends FilterInputStream {
    private static final int BOUNDARY_NOT_FOUND = SimpleBoyerMoore.PATTERN_NOT_FOUND;

    private byte[] boundary;

    private SimpleBoyerMoore boyerMoore;

    private byte[] leftOvers;

    private int leftOverPosition;

    private InputStream source;

    private boolean simulateEof;

    private boolean realEof;

    private boolean bufferingCompleted;

    /**
     * Constructs a <code>BoundaryDelimitedInputStream</code> using the passed <code>InputStream</code> as the source for the
     * outer stream.
     *
     * @param in the source input stream
     * @param boundary the byte boundary separating sections of this stream
     */
    public BoundaryDelimitedInputStream(InputStream in, byte[] boundary) {
        super(in);
        source = in;
        this.boundary = (byte[]) boundary.clone();
        boyerMoore = new SimpleBoyerMoore(this.boundary);
    }

    /*
     * Buffers read-ahead data for future read calls.
     */
    private void createLeftOvers(byte[] buffer, int start, int end) {
        int length = end - start;

        if (length <= 0)
            return;

        // If we are no longer buffering, we no longer have to expand the buffer, so we
        // can reuse it (if its still there, which may not be the case if a boundary was found
        // after the left over buffer was consumed) by just moving the position back.
        if (bufferingCompleted && leftOvers != null) {
            leftOverPosition -= length;
            return;
        }

        int leftOverLength = (leftOvers == null) ? 0 : leftOvers.length - leftOverPosition;

        byte[] newLeftOvers = new byte[length + leftOverLength];

        System.arraycopy(buffer, start, newLeftOvers, 0, length);

        if (leftOvers != null)
            System.arraycopy(leftOvers, leftOverPosition, newLeftOvers, length, leftOverLength);

        leftOvers = newLeftOvers;
        leftOverPosition = 0;
    }

    /*
     * Reads from previous buffered read-ahead data
     */
    private int consumeLeftOvers(byte[] buffer, int offset, int length) {
        if (leftOvers == null)
            return 0;

        int i = 0;
        while (i < length && leftOverPosition < leftOvers.length) {
            buffer[offset + i++] = leftOvers[leftOverPosition++];
        }

        if (leftOverPosition >= leftOvers.length) {
            leftOvers = null;
            leftOverPosition = 0;
        }

        return i;
    }

    /*
     * Repeatably reads from the source stream until the desired number of bytes are returned.
     */
    private int fullRead(byte[] buffer, int offset, int length) throws IOException {
        int count = 0;
        int read = 0;

        do {
            read = source.read(buffer, offset + count, length - count);
            if (read > 0)
                count += read;
        } while (read > 0 && count < length);

        if (read < 0)
            realEof = true;

        return count;
    }

    private int findBoundary(byte[] buffer, int length) {
        return boyerMoore.patternSearch(buffer, 0, length);
    }

    private int read(byte[] b, int off, int len, boolean skip) throws IOException {
        if (len == 0)
            return 0;

        if (simulateEof) {
            simulateEof = false;
            return -1;
        }

        if (realEof) {
            if (leftOvers == null)
                return -1;
            if (bufferingCompleted == false) {
                bufferingCompleted = true;
            }
        }

        // Our buffer must always contain room for 2 boundary string occurrences,
        // and one of those boundary string size chunks must extend past the length
        // of the requested read size to insure the returned byte chunk contains
        // no portion of the boundary.
        int bufferLength = Math.max(boundary.length * 2, len + boundary.length);

        byte[] buffer = new byte[bufferLength];

        int position = consumeLeftOvers(buffer, 0, bufferLength);
        if (position < bufferLength)
            position += fullRead(buffer, position, bufferLength - position);
        // This should only occur when the source stream is already closed at start
        if (realEof && position == 0)
            return -1;

        int returnLength;
        int boundaryPosition = findBoundary(buffer, position);

        if (boundaryPosition == BOUNDARY_NOT_FOUND || boundaryPosition >= len) {
            returnLength = Math.min(len, position);
            createLeftOvers(buffer, returnLength, position);
        } else {
            returnLength = boundaryPosition;
            createLeftOvers(buffer, returnLength + boundary.length, position);

            // If there is no data to return, send the eof immediately
            if (returnLength == 0)
                return -1;

            // Notify the client that this inner stream has ended by returning -1 on the
            // next read request.
            simulateEof = true;
        }

        if (!skip)
            System.arraycopy(buffer, 0, b, off, returnLength);

        return returnLength;
    }

    /**
     * This method will always return 0 because this input stream must always read ahead to determine the location of the
     * boundary.
     */
    public int available() throws IOException {
        return 0;
    }

    /**
     * Close this input stream, and its source input stream. Once this is called, no further data can be read.
     */
    public void close() throws IOException {
        source.close();
        leftOvers = null;
        leftOverPosition = 0;
        realEof = true;
    }

    /**
     * Returns false. Mark is not support by this input stream.
     */
    public boolean markSupported() {
        return false;
    }

    /**
     * Reads a single byte from the inner input stream. See the general contract of the read method of <code>InputStream</code>.
     *
     * @return a single byte value from the stream in the range of 0-255 or -1 on eof of the inner stream.
     */
    public int read() throws IOException {
        byte[] b = new byte[1];
        if (read(b) == -1)
            return -1;
        return b[0] & 0xff;
    }

    /**
     * Reads from the inner input stream, attempting to fill the passed byte array. See the general contract of the read method
     * of <code>InputStream</code>.
     *
     * @param b the byte array to populate
     * @return number of bytes returned in <code>b</code>, -1 on EOF
     */
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Skips the specified number of bytes from the inner input stream. See the general contract of the read method of
     * <code>InputStream</code>.
     *
     * @param n the number of bytes to skip
     * @return the number of bytes actually skipped, -1 on EOF
     */
    public long skip(long n) throws IOException {
        return read(null, 0, (int) n, true);
    }

    /**
     * Reads the specified number of bytes starting from the specified offset into the specified buffer from the inner input
     * stream. See the general contract of the read method of <code>InputStream</code>.
     *
     * @param b the byte array to populate
     * @param off the offset in the array to start at
     * @param len the number of bytes to read, -1 on EOF
     */
    public int read(byte[] b, int off, int len) throws IOException {
        return read(b, off, len, false);
    }

    /**
     * Returns whether the outer stream is closed.
     *
     * @return boolean indicating whether the outer stream is closed
     */
    public boolean isOuterStreamClosed() {
        return realEof && leftOvers == null;
    }

    /**
     * Sets a new boundary to delimit the stream by. This can be performed at any time.
     * Obviously, data that has already been returned can not be guaranteed not to
     * contain the new boundary.
     *
     * @param boundary
     */
    public void setBoundary(byte[] boundary) {
        this.boundary = boundary.clone();
        boyerMoore = new SimpleBoyerMoore(boundary);
    }

    public void printLeftOvers() {
        if (leftOvers != null)
            ROOT_LOGGER.debugf("LEFT = %s", new String(leftOvers, leftOverPosition, leftOvers.length - leftOverPosition));
    }
}