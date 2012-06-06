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
package org.jboss.as.controller.remote;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PipedInputStream;

import org.xnio.IoUtils;

/**
 * An in-VM pipe between an input stream and an output stream, which does not suffer from the
 * bugs in {@link PipedInputStream}.
 *
 * This is forked from {@link org.xnio.streams.Pipe} to throw an exception if the writer kills the pipe
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:david.lloyd@redhat.com">Kabir Khan</a>
 */
final class Pipe {
    private final Object lock = new Object();

    /** the point at which a read shall occur **/
    private int tail;
    /** the size of the buffer content **/
    private int size;
    private final byte[] buffer;
    private boolean writeClosed;
    private boolean readClosed;
    private boolean killed;

    /**
     * Construct a new instance.
     *
     * @param bufferSize the buffer size to use
     */
    public Pipe(int bufferSize) {
        buffer = new byte[bufferSize];
    }

    /**
     * Wait for the read side to close.  Used when the writer needs to know when
     * the reader finishes consuming a message.
     */
    public void await() {
        boolean intr = false;
        final Object lock = this.lock;
        try {
            synchronized (lock) {
                while (! readClosed) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        intr = true;
                    }
                }
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void kill() {
        synchronized (lock) {
            killed = true;
            IoUtils.safeClose(out);
            IoUtils.safeClose(in);
        }
    }

    private final InputStream in = new InputStream() {
        public int read() throws IOException {
            final Object lock = Pipe.this.lock;
            synchronized (lock) {
                if (killed) {
                    throw MESSAGES.streamWasKilled();
                }
                if (writeClosed && size == 0) {
                    return -1;
                }
                while (size == 0) {
                    try {
                        lock.wait();
                        if (killed) {
                            throw MESSAGES.streamWasKilled();
                        }
                        if (writeClosed && size == 0) {
                            return -1;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedIOException();
                    }
                }
                lock.notifyAll();
                int tail= Pipe.this.tail;
                try {
                    return buffer[tail++] & 0xff;
                } finally {
                    Pipe.this.tail = tail == buffer.length ? 0 : tail;
                    size--;
                }
            }
        }

        public int read(final byte[] b, final int off, final int len) throws IOException {
            final Object lock = Pipe.this.lock;
            synchronized (lock) {
                if (killed) {
                    throw MESSAGES.streamWasKilled();
                }
                if (writeClosed && size == 0) {
                    return -1;
                }
                if (len == 0) {
                    return 0;
                }
                int size;
                while ((size = Pipe.this.size) == 0) {
                    try {
                        lock.wait();
                        if (killed) {
                            throw MESSAGES.streamWasKilled();
                        }
                        if (writeClosed && (size = Pipe.this.size) == 0) {
                            return -1;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedIOException();
                    }
                }
                final byte[] buffer = Pipe.this.buffer;
                final int bufLen = buffer.length;
                int cnt;
                int tail = Pipe.this.tail;
                if (size + tail > bufLen) {
                    // wrapped
                    final int lastLen = bufLen - tail;
                    if (lastLen < len) {
                        final int firstLen = tail + size - bufLen;
                        System.arraycopy(buffer, tail, b, off, lastLen);
                        int rem = Math.min(len - lastLen, firstLen);
                        System.arraycopy(buffer, 0, b, off + lastLen, rem);
                        cnt = rem + lastLen;
                    } else {
                        System.arraycopy(buffer, tail, b, off, len);
                        cnt = len;
                    }
                } else {
                    // not wrapped
                    cnt = Math.min(len, size);
                    System.arraycopy(buffer, tail, b, off, cnt);
                }
                tail += cnt;
                size -= cnt;
                Pipe.this.tail = tail >= bufLen ? tail - bufLen : tail;
                Pipe.this.size = size;
                lock.notifyAll();
                return cnt;
            }
        }

        public void close() throws IOException {
            final Object lock = Pipe.this.lock;
            synchronized (lock) {
                writeClosed = true;
                readClosed = true;
                // closing the read side drops the remaining bytes
                size = 0;
                lock.notifyAll();
                return;
            }
        }
    };
    final OutputStream out = new OutputStream() {
        public void write(final int b) throws IOException {
            final Object lock = Pipe.this.lock;
            synchronized (lock) {
                if (killed) {
                    throw MESSAGES.streamWasKilled();
                }
                if (writeClosed) {
                    throw MESSAGES.streamWasClosed();
                }
                final byte[] buffer = Pipe.this.buffer;
                final int bufLen = buffer.length;
                while (size == bufLen) {
                    try {
                        lock.wait();
                        if (killed) {
                            throw MESSAGES.streamWasKilled();
                        }
                        if (writeClosed) {
                            throw MESSAGES.streamWasClosed();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedIOException();
                    }
                }
                final int tail = Pipe.this.tail;
                int startPos = tail + size;
                if (startPos >= bufLen) {
                    buffer[startPos - bufLen] = (byte) b;
                } else {
                    buffer[startPos] = (byte) b;
                }
                size ++;
                lock.notifyAll();
            }
        }

        public void write(final byte[] b, int off, final int len) throws IOException {
            int remaining = len;
            final Object lock = Pipe.this.lock;
            synchronized (lock) {
                if (killed) {
                    throw MESSAGES.streamWasKilled();
                }
                if (writeClosed) {
                    throw MESSAGES.streamWasClosed();
                }
                final byte[] buffer = Pipe.this.buffer;
                final int bufLen = buffer.length;
                int size;
                int tail;
                int cnt;
                while (remaining > 0) {
                    while ((size = Pipe.this.size) == bufLen) {
                        try {
                            lock.wait();
                            if (killed) {
                                throw MESSAGES.streamWasKilled();
                            }
                            if (writeClosed) {
                                throw MESSAGES.streamWasClosed();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            final InterruptedIOException iioe = new InterruptedIOException();
                            iioe.bytesTransferred = len - remaining;
                            throw iioe;
                        }
                    }
                    tail = Pipe.this.tail;
                    int startPos = tail + size;
                    if (startPos >= bufLen) {
                        // read wraps, write doesn't
                        startPos -= bufLen;
                        cnt = Math.min(remaining, bufLen - size);
                        System.arraycopy(b, off, buffer, startPos, cnt);
                        remaining -= cnt;
                        off += cnt;
                    } else {
                        // write wraps, read doesn't
                        final int firstPart = Math.min(remaining, bufLen - (tail + size));
                        System.arraycopy(b, off, buffer, startPos, firstPart);
                        off += firstPart;
                        remaining -= firstPart;
                        if (remaining > 0) {
                            final int latter = Math.min(remaining, tail);
                            System.arraycopy(b, off, buffer, 0, latter);
                            cnt = firstPart + latter;
                            off += latter;
                            remaining -= latter;
                        } else {
                            cnt = firstPart;
                        }
                    }
                    Pipe.this.size += cnt;
                    lock.notifyAll();
                }
            }
        }

        public void close() throws IOException {
            final Object lock = Pipe.this.lock;
            synchronized (lock) {
                writeClosed = true;
                lock.notifyAll();
                return;
            }
        }
    };



    /**
     * Get the input (read) side of the pipe.
     *
     * @return the input side
     */
    public InputStream getIn() {
        return in;
    }

    /**
     * Get the output (write) side of the pipe.
     *
     * @return the output side
     */
    public OutputStream getOut() {
        return out;
    }
}
