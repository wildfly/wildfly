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
import java.io.InputStream;
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
import org.jboss.as.protocol.SimpleByteDataInput;
import org.jboss.as.protocol.SimpleByteDataOutput;
import static org.jboss.as.protocol.StreamUtils.safeClose;

/**
 * Base management request used for remote requests.  Provides the basic mechanism for connecting to a remote server manager
 * for performing a task.  It will manage connecting and retreiving the correct response.
 *
 * @author John Bailey
 */
public abstract class ManagementRequest<T> implements MessageHandler {
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
        try {
            connection = protocolClient.connect();
            dataOutput = connection.writeMessage();
            final ByteDataOutput output = new SimpleByteDataOutput(dataOutput);

            // Start by writing the header
            final ManagementRequestHeader managementRequestHeader = new ManagementRequestHeader(ManagementProtocol.VERSION, requestId, getHandlerId());
            managementRequestHeader.write(output);
        } catch (IOException e) {
            throw new ManagementException("Failed to connect using protocol client", e);
        } finally {
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

    public void handleShutdown(Connection connection) throws IOException {
        safeClose(connection);
    }

    public void handleFailure(Connection connection, IOException e) throws IOException {

    }

    public void handleFinished(Connection connection) throws IOException {
        // NOOP
    }

    public void handleMessage(Connection connection, InputStream dataStream) throws IOException {
        try {
            future.set(receiveResponse(connection, dataStream));
        } catch (ManagementException e) {
            future.setException(e);
        } catch (Throwable t) {
            future.setException(new ManagementException("Failed to handle management message", t));
        } finally {
            safeClose(dataStream);
        }
    }

    private class InitiatingMessageHandler implements MessageHandler {
        public void handleMessage(Connection connection, InputStream dataStream) throws IOException {
            // First read the response header
            final ByteDataInput input = new SimpleByteDataInput(dataStream);
            final ManagementResponseHeader responseHeader;
            try {
                responseHeader = new ManagementResponseHeader(input);

                if (requestId != responseHeader.getResponseId()) {
                    throw new IOException("Invalid request ID expecting " + requestId + " received " + responseHeader.getResponseId());
                }

                connection.setMessageHandler(ManagementRequest.this);
                sendRequest(responseHeader.getVersion(), connection);

            } catch (ManagementException e) {
                throw new IOException("Failed to read response header", e);
            }
        }

        public void handleShutdown(Connection connection) throws IOException {
            ManagementRequest.this.handleShutdown(connection);
        }

        public void handleFailure(Connection connection, IOException e) throws IOException {
            ManagementRequest.this.handleFailure(connection, e);
        }

        public void handleFinished(Connection connection) throws IOException {
            ManagementRequest.this.handleFinished(connection);
        }
    }

    /**
     * Execute the request body.  This is run after the connection is established and the headers are exchanged.
     *
     * @param protocolVersion The active protocol version for the request
     * @param connection      The connection
     * @throws ManagementException If any errors occur
     */
    protected abstract void sendRequest(final int protocolVersion, final Connection connection) throws ManagementException;

    /**
     * Receive the response from the request.
     *
     * @param connection The connection
     * @param dataStream The dataStream to read from
     * @return The result of the operation
     * @throws ManagementException If any errors occur
     */
    protected abstract T receiveResponse(final Connection connection, final InputStream dataStream) throws ManagementException;

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
