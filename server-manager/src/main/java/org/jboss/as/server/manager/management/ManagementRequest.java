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

package org.jboss.as.server.manager.management;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.SocketFactory;
import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.ProtocolClient;
import org.jboss.as.protocol.SimpleByteDataOutput;
import static org.jboss.as.protocol.StreamUtils.safeClose;
import static org.jboss.as.server.manager.management.ManagementUtils.expectHeader;

/**
 * Base management request used for remote requests.  Provides the basic mechanism for connecting to a remote server manager
 * for performing a task.  It will manage connecting and retreiving the correct response.
 *
 * @author John Bailey
 */
public abstract class ManagementRequest<T> extends AbstractMessageHandler {
    private final InetAddress address;
    private final int port;
    private final long connectTimeout;
    private final ScheduledExecutorService executorService;
    private final ThreadFactory threadFactory;
    private Connection connection;
    private int requestId = 0;
    private final ResponseFuture<T> future = new ResponseFuture<T>();

    /**
     * Construct a new request object with the required connection parameters.
     *
     * @param address                 The remote address to connect to
     * @param port                    The remote port to connect to
     * @param connectTimeout          The timeout for connecting
     * @param executorService         The executor server to schedule tasks with
     * @param threadFactory           The connection thread factory
     */
    public ManagementRequest(InetAddress address, int port, long connectTimeout, final ScheduledExecutorService executorService, final ThreadFactory threadFactory) {
        this.address = address;
        this.port = port;
        this.connectTimeout = connectTimeout;
        this.executorService = executorService;
        this.threadFactory = threadFactory;
    }

    /**
     * Get the handler id of the request.  These should match the id of a @{link org.jboss.as.server.manager.management.ManagementOperationHandler}.
     *
     * @return The handler id
     */
    protected abstract byte getHandlerId();

    /**
     * Execute the request by connecting and then delegating to the implementation's execute
     * and return a future used to get the response when complete.
     *
     * @return A future to retrieve the result when the request is complete
     * @throws ManagementException If any problems occur with the request
     */
    public final Future<T> execute() throws ManagementException {
        final int timeout = (int) TimeUnit.SECONDS.toMillis(connectTimeout);

        final ProtocolClient.Configuration config = new ProtocolClient.Configuration();
        config.setMessageHandler(new InitiatingMessageHandler());
        config.setConnectTimeout(timeout);
        config.setReadExecutor(executorService);
        config.setSocketFactory(SocketFactory.getDefault());
        config.setServerAddress(new InetSocketAddress(address, port));
        config.setThreadFactory(threadFactory);

        final ProtocolClient protocolClient = new ProtocolClient(config);
        OutputStream dataOutput = null;
        ByteDataOutput output = null;
        try {
            connection = protocolClient.connect();
            dataOutput = connection.writeMessage();
            output = new SimpleByteDataOutput(dataOutput);

            // Start by writing the header
            final ManagementRequestHeader managementRequestHeader = new ManagementRequestHeader(ManagementProtocol.VERSION, requestId, getHandlerId());
            managementRequestHeader.write(output);
        } catch (IOException e) {
            throw new ManagementException("Failed to connect using protocol client", e);
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
     * @throws ManagementException If any problems occur
     */
    public T executeForResult() throws ManagementException {
        try {
            return execute().get();
        } catch (ManagementException e) {
            throw e;
        } catch (Exception e) {
            throw new ManagementException("Failed to execute remote request", e);
        }
    }

    public void handle(Connection connection, ByteDataInput input) throws ManagementException {
        try {
            expectHeader(input, ManagementProtocol.RESPONSE_START);
            byte responseCode = input.readByte();
            if (responseCode != getResponseCode()) {
                throw new ManagementException("Invalid response code.  Expecting '" + getResponseCode() + "' received '" + responseCode + "'");
            }
            connection.setMessageHandler(responseBodyHandler);
        } catch (ManagementException e) {
            future.setException(e);
        } catch (Throwable t) {
            future.setException(new ManagementException("Failed to handle management message", t));
        }
    }

    private class InitiatingMessageHandler extends AbstractMessageHandler {
        void handle(final Connection connection, final ByteDataInput input) throws ManagementException {
            final ManagementResponseHeader responseHeader;
            try {
                responseHeader = new ManagementResponseHeader(input);
                if (requestId != responseHeader.getResponseId()) {
                    throw new ManagementException("Invalid request ID expecting " + requestId + " received " + responseHeader.getResponseId());
                }
                connection.setMessageHandler(ManagementRequest.this);
                sendRequest(responseHeader.getVersion(), connection);
            } catch (IOException e) {
                throw new ManagementException("Failed to read response header", e);
            }
        }
    }

    /**
     * Execute the request body.  This is run after the connection is established and the headers are exchanged.
     *
     * @param protocolVersion The active protocol version for the request
     * @param connection      The connection
     * @throws ManagementException If any errors occur
     */
    protected void sendRequest(final int protocolVersion, final Connection connection) throws ManagementException {
        OutputStream outputStream = null;
        ByteDataOutput output = null;
        try {
            try {
                outputStream = connection.writeMessage();
                output = new SimpleByteDataOutput(outputStream);
                output.writeByte(ManagementProtocol.REQUEST_OPERATION);
                output.writeByte(getRequestCode());
                output.writeByte(ManagementProtocol.REQUEST_START);
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
                output.writeByte(ManagementProtocol.REQUEST_END);
            } finally {
                safeClose(output);
                safeClose(outputStream);
            }
        } catch (ManagementException e) {
            throw e;
        } catch (Throwable t) {
            throw new ManagementException("Failed to send management request", t);
        }
    }

    protected void sendRequest(final int protocolVersion, final ByteDataOutput output) throws ManagementException {
    }

    protected abstract byte getRequestCode();

    protected abstract byte getResponseCode();

    private MessageHandler responseBodyHandler = new AbstractMessageHandler() {
        void handle(Connection connection, ByteDataInput input) throws ManagementException {
            future.set(receiveResponse(input));
            connection.setMessageHandler(responseEndHandler);
        }
    };

    private MessageHandler responseEndHandler = new AbstractMessageHandler() {
        void handle(Connection connection, ByteDataInput input) throws ManagementException {
            try {
                expectHeader(input, ManagementProtocol.RESPONSE_END);
                connection.setMessageHandler(MessageHandler.NULL);
            } catch (IOException e) {
                throw new ManagementException("Failed to read response end", e);
            }
        }
    };

    protected T receiveResponse(final ByteDataInput input) throws ManagementException {
        return null;
    }

    private final class ResponseFuture<T> implements Future<T>{
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
