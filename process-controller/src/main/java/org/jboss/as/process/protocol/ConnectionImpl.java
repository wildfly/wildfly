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

import static org.jboss.as.process.protocol.ProtocolLogger.CONNECTION_LOGGER;
import static org.jboss.as.process.protocol.ProtocolMessages.MESSAGES;

import java.io.BufferedOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ConnectionImpl implements Connection {

    private final Socket socket;

    private final Object lock = new Object();

    // protected by {@link #lock}
    private OutputStream sender;
    // protected by {@link #lock}
    private boolean readDone;
    // protected by {@link #lock}
    private boolean writeDone;

    private volatile MessageHandler messageHandler;

    private final Executor readExecutor;

    private volatile Object attachment;

    private volatile MessageHandler backupHandler;

    private final ClosedCallback callback;

    ConnectionImpl(final Socket socket, final MessageHandler handler, final Executor readExecutor, final ClosedCallback callback) {
        this.socket = socket;
        messageHandler = handler;
        this.readExecutor = readExecutor;
        this.callback = callback;
    }

    @Override
    public OutputStream writeMessage() throws IOException {
        final OutputStream os;
        synchronized (lock) {
            if (writeDone) {
                throw MESSAGES.writesAlreadyShutdown();
            }
            while (sender != null) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException();
                }
            }
            boolean ok = false;
            try {
                sender = new MessageOutputStream();
                os = new BufferedOutputStream(sender);
                ok = true;
            } finally {
                if (! ok) {
                    // let someone else try
                    lock.notify();
                }
            }
        }
        return os;
    }

    @Override
    public void shutdownWrites() throws IOException {
        synchronized (lock) {
            if (writeDone) return;
            while (sender != null) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException();
                }
            }
            writeDone = true;
            if (readDone) {
                socket.close();
            } else {
                socket.shutdownOutput();
            }
            lock.notifyAll();
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            lock.notifyAll();
            sender = null;
            readDone = true;
            writeDone = true;
            socket.close();
            lock.notifyAll();
        }
    }

    @Override
    public void setMessageHandler(final MessageHandler messageHandler) {
        if (messageHandler == null) {
            throw MESSAGES.nullVar("messageHandler");
        }
        this.messageHandler = messageHandler;
    }

    @Override
    public InetAddress getPeerAddress() {
        synchronized (lock) {
            final Socket socket = this.socket;
            if (socket != null) {
                return socket.getInetAddress();
            } else {
                return null;
            }
        }
    }

    @Override
    public void attach(final Object attachment) {
        this.attachment = attachment;
    }

    @Override
    public Object getAttachment() {
        return attachment;
    }

    @Override
    public void backupMessageHandler() {
        backupHandler = messageHandler;
    }

    @Override
    public void restoreMessageHandler() {
        MessageHandler handler = backupHandler;
        setMessageHandler(handler == null ? MessageHandler.NULL : handler);
    }

    Runnable getReadTask() {
        return new Runnable() {
            @Override
            public void run() {
                boolean closed = false;
                OutputStream mos = null;
                try {
                    Pipe pipe = null;
                    final InputStream is = socket.getInputStream();
                    final int bufferSize = 8192;
                    final byte[] buffer = new byte[bufferSize];
                    for (;;) {

                        int cmd = is.read();
                        switch (cmd) {
                            case -1: {
                                CONNECTION_LOGGER.trace("Received end of stream");
                                // end of stream
                                safeHandleShutdown();
                                boolean done;
                                if (mos != null) {
                                    mos.close();
                                    pipe.await();
                                }
                                synchronized (lock) {
                                    readDone = true;
                                    done = writeDone;
                                }
                                if (done) {
                                    StreamUtils.safeClose(socket);
                                    safeHandleFinished();
                                }
                                closed = true;
                                closed();
                                return;
                            }
                            case ProtocolConstants.CHUNK_START: {
                                if (mos == null) {
                                    pipe = new Pipe(8192);
                                    // new message!
                                    final InputStream pis = pipe.getIn();
                                    mos = pipe.getOut();

                                    readExecutor.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            safeHandleMessage(new MessageInputStream(pis));
                                        }
                                    });
                                }
                                int cnt = StreamUtils.readInt(is);
                                CONNECTION_LOGGER.tracef("Received data chunk of size %d", Integer.valueOf(cnt));
                                while (cnt > 0) {
                                    int sc = is.read(buffer, 0, Math.min(cnt, bufferSize));
                                    if (sc == -1) {
                                        throw MESSAGES.unexpectedEndOfStream();
                                    }
                                    mos.write(buffer, 0, sc);
                                    cnt -= sc;
                                }
                                break;
                            }
                            case ProtocolConstants.CHUNK_END: {
                                CONNECTION_LOGGER.trace("Received end data marker");
                                if (mos != null) {
                                    // end message
                                    mos.close();
                                    pipe.await();
                                    mos = null;
                                    pipe = null;
                                }
                                break;
                            }
                            default: {
                                throw MESSAGES.invalidCommandByte(cmd);
                            }
                        }
                    }
                } catch (IOException e) {
                    safeHandlerFailure(e);
                } finally {
                    StreamUtils.safeClose(mos);
                    if (!closed) {
                        closed();
                    }
                }
            }
        };
    }

    void safeHandleMessage(final InputStream pis) {
        try {
            messageHandler.handleMessage(this, pis);
        } catch (RuntimeException e) {
            CONNECTION_LOGGER.failedToReadMessage(e);
        } catch (IOException e) {
            CONNECTION_LOGGER.failedToReadMessage(e);
        } catch (NoClassDefFoundError e) {
            CONNECTION_LOGGER.failedToReadMessage(e);
        } catch (Error e) {
            CONNECTION_LOGGER.failedToReadMessage(e);
            throw e;
        } finally {
            StreamUtils.safeClose(pis);
        }
    }

    void safeHandleShutdown() {
        try {
            messageHandler.handleShutdown(this);
        } catch (IOException e) {
            CONNECTION_LOGGER.failedToHandleSocketShutdown(e);
        }
    }

    void safeHandleFinished() {
        try {
            messageHandler.handleFinished(this);
        } catch (IOException e) {
            CONNECTION_LOGGER.failedToHandleSocketFinished(e);
        }
    }

    void safeHandlerFailure(IOException e) {
        try {
            messageHandler.handleFailure(this, e);
        } catch (IOException e1) {
            CONNECTION_LOGGER.failedToHandleSocketFailure(e);
        }
    }

    final class MessageInputStream extends FilterInputStream {

        protected MessageInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            try {
                while (in.read() != -1) {}
            } finally {
                super.close();
            }
        }
    }

    final class MessageOutputStream extends FilterOutputStream {

        private final byte[] hdr = new byte[5];

        MessageOutputStream() throws IOException {
            super(socket.getOutputStream());
        }

        @Override
        public void write(final int b) throws IOException {
            throw new IllegalStateException();
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            if (len == 0) {
                return;
            }
            final byte[] hdr = this.hdr;
            hdr[0] = (byte) ProtocolConstants.CHUNK_START;
            hdr[1] = (byte) (len >> 24);
            hdr[2] = (byte) (len >> 16);
            hdr[3] = (byte) (len >> 8);
            hdr[4] = (byte) (len >> 0);
            synchronized (lock) {
                if (sender != this || writeDone) {
                    if (sender == this) sender = null;
                    lock.notifyAll();
                    throw MESSAGES.writeChannelClosed();
                }
                CONNECTION_LOGGER.tracef("Sending data chunk of size %d", Integer.valueOf(len));
                out.write(hdr);
                out.write(b, off, len);
            }
        }

        @Override
        public void close() throws IOException {
            synchronized (lock) {
                if (sender != this) {
                    return;
                }
                sender = null;
                // wake up waiters
                lock.notify();
                if (writeDone) throw MESSAGES.writeChannelClosed();
                if (readDone) {
                    readExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            safeHandleFinished();
                        }
                    });
                }
                CONNECTION_LOGGER.tracef("Sending end of message");
                out.write(ProtocolConstants.CHUNK_END);
            }
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            synchronized (lock) {
                if (sender == this) {
                    CONNECTION_LOGGER.leakedMessageOutputStream();
                    close();
                }
            }
        }
    }

    private void closed() {
        ClosedCallback callback = this.callback;
        if (callback != null) {
            callback.connectionClosed();
        }
    }
}
