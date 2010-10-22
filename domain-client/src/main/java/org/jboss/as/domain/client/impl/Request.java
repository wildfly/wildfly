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

package org.jboss.as.domain.client.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.SocketFactory;
import static org.jboss.as.domain.client.impl.ProtocolUtils.expectHeader;
import static org.jboss.as.domain.client.impl.ProtocolUtils.readResponseHeader;
import static org.jboss.as.domain.client.impl.ProtocolUtils.writeRequestHeader;
import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.ProtocolClient;
import org.jboss.as.protocol.SimpleByteDataOutput;
import static org.jboss.as.protocol.StreamUtils.safeClose;

/**
 * @author John Bailey
 */
abstract class Request<T> extends AbstractMessageHandler {
    private final ResponseFuture<T> future = new ResponseFuture<T>();
    private final InetAddress address;
    private final int port;
    private final ExecutorService executorService;
    private final ThreadFactory threadFactory;

    Request(InetAddress address, int port, ExecutorService executorService, ThreadFactory threadFactory) {
        this.address = address;
        this.port = port;
        this.executorService = executorService;
        this.threadFactory = threadFactory;
    }

    public final Future<T> execute() throws Exception {
        final int timeout = (int) TimeUnit.SECONDS.toMillis(5L);
        final ProtocolClient.Configuration config = new ProtocolClient.Configuration();
        config.setMessageHandler(new InitiatingMessageHandler<T>(this));
        config.setConnectTimeout(timeout);
        config.setReadExecutor(executorService);
        config.setSocketFactory(SocketFactory.getDefault());
        config.setServerAddress(new InetSocketAddress(address, port));
        config.setThreadFactory(threadFactory);

        final ProtocolClient protocolClient = new ProtocolClient(config);
        OutputStream dataOutput = null;
        ByteDataOutput output = null;
        try {
            final Connection connection = protocolClient.connect();
            dataOutput = connection.writeMessage();
            output = new SimpleByteDataOutput(dataOutput);
            writeRequestHeader(output);
        } finally {
            safeClose(output);
            safeClose(dataOutput);
        }
        return future;
    }

    /**
     * Execute the request and wait for the result.
     *
     * @return The result
     */
    public T executeForResult() {
        try {
            return execute().get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute remote request", e);
        }
    }

    void handle(final Connection connection, final ByteDataInput input) throws IOException {
        try {
            expectHeader(input, Protocol.RESPONSE_START);
            byte responseCode = input.readByte();
            if (responseCode != getResponseCode()) {
                throw new IOException("Invalid response code.  Expecting '" + getResponseCode() + "' received '" + responseCode + "'");
            }
            connection.setMessageHandler(responseBodyHandler);
        } catch (IOException e) {
            future.setException(e);
        } catch (Throwable t) {
            future.setException(new IOException("Failed to handle management message", t));
        }
    }

    protected abstract byte getRequestCode();

    protected abstract byte getResponseCode();

    protected void sendRequest(final int protocolVersion, final Connection connection) throws IOException {
        OutputStream outputStream = null;
        ByteDataOutput output = null;
        try {
            outputStream = connection.writeMessage();
            output = new SimpleByteDataOutput(outputStream);
            output.writeByte(Protocol.REQUEST_OPERATION);
            output.writeByte(getRequestCode());
            output.writeByte(Protocol.REQUEST_START);
        } finally {
            safeClose(output);
            safeClose(outputStream);
        }

        try {
            outputStream = connection.writeMessage();
            output = new SimpleByteDataOutput(outputStream);
            sendRequest(protocolVersion, output);
        } finally {
            safeClose(output);
            safeClose(outputStream);
        }

        try {
            outputStream = connection.writeMessage();
            output = new SimpleByteDataOutput(outputStream);
            output.writeByte(Protocol.REQUEST_END);
        } finally {
            safeClose(output);
            safeClose(outputStream);
        }
    }

    protected void sendRequest(final int protocolVersion, final ByteDataOutput output) throws IOException {

    }

    protected T receiveResponse(final ByteDataInput input) {
        return null;
    }

    private MessageHandler responseBodyHandler = new AbstractMessageHandler() {
        void handle(Connection connection, ByteDataInput input) throws IOException {
            future.set(receiveResponse(input));
            connection.setMessageHandler(responseEndHandler);
        }
    };

    private MessageHandler responseEndHandler = new AbstractMessageHandler() {
        void handle(Connection connection, ByteDataInput input) throws IOException {
            expectHeader(input, Protocol.RESPONSE_END);
            connection.setMessageHandler(MessageHandler.NULL);
        }
    };

    private class InitiatingMessageHandler<T> extends AbstractMessageHandler {
        private final Request<T> request;

        private InitiatingMessageHandler(Request<T> request) {
            this.request = request;
        }

        void handle(Connection connection, ByteDataInput input) throws IOException {
            final int workingVersion = readResponseHeader(input);
            connection.setMessageHandler(request);
            request.sendRequest(workingVersion, connection);
        }
    }

    private final class ResponseFuture<T> implements Future<T> {
        private volatile T result;
        private volatile Exception exception;

        public T get() throws InterruptedException, ExecutionException {
            boolean intr = false;
            try {
                synchronized (this) {
                    while (this.result == null && exception == null) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            intr = true;
                        }
                    }
                }
                if (exception != null) {
                    throw new ExecutionException(exception);
                }
                return result;
            } finally {
                if (intr) Thread.currentThread().interrupt();
            }
        }


        void set(final T result) {
            synchronized (this) {
                this.result = result;
                notifyAll();
            }
        }

        void setException(final Exception exception) {
            synchronized (this) {
                this.exception = exception;
                notifyAll();
            }
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        public boolean isCancelled() {
            return false;
        }

        public synchronized boolean isDone() {
            return result != null || exception != null;
        }

        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }
}
