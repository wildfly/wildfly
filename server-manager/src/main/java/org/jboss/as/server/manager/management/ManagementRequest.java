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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.SimpleByteDataInput;
import org.jboss.as.protocol.SimpleByteDataOutput;

/**
 * Base management request used for remote requests.  Provides the basic mechanism for connecting to a remote server manager
 * for performing a task.  It will manage connecting and retreiving the correct response.
 *
 * @author John Bailey
 */
public abstract class ManagementRequest<T> {
    private final InetAddress address;
    private final int port;
    private final int connectionRetryLimit;
    private final long connectionRetryInterval;
    private final long connectTimeout;
    private final ScheduledExecutorService executorService;

    /**
     * Construct a new request object with the required connection parameters.
     *
     * @param address The remote address to connect to
     * @param port The remote port to connect to
     * @param connectionRetryLimit The connection retry limit
     * @param connectionRetryInterval The interval between connection attempts
     * @param connectTimeout The timeout for connecting
     * @param executorService The executor server to schedule tasks with
     */
    public ManagementRequest(InetAddress address, int port, int connectionRetryLimit, long connectionRetryInterval, long connectTimeout, ScheduledExecutorService executorService) {
        this.address = address;
        this.port = port;
        this.connectionRetryLimit = connectionRetryLimit;
        this.connectionRetryInterval = connectionRetryInterval;
        this.connectTimeout = connectTimeout;
        this.executorService = executorService;
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
        final InitiatingFuture<T> initiatingFuture = new InitiatingFuture<T>();
        executorService.execute(new InitiateRequestTask<T>(this, initiatingFuture));
        return initiatingFuture.get();
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

    /**
     * Execute the request body.  This is run after the connection is established and the headers are exchanged.
     *
     * @param protocolVersion The active protocol version for the request
     * @param output The output to write to
     * @param input The input to read from
     * @return The result of the request
     * @throws ManagementException If any errors occur
     */
    protected abstract T execute(final int protocolVersion, final ByteDataOutput output, final ByteDataInput input) throws ManagementException;

    private class InitiateRequestTask<T> implements Runnable {
        private final ManagementRequest<T> request;
        private final InitiatingFuture<T> initiatingFuture;
        private int requestId;

        private InitiateRequestTask(final ManagementRequest<T> request, final InitiatingFuture<T> initiatingFuture) {
            this.request = request;
            this.initiatingFuture = initiatingFuture;
        }

        public final void run() {
            final int requestId = this.requestId++;
            Socket socket = null;
            try {
                socket = new Socket();
                final int timeout = (int) TimeUnit.SECONDS.toMillis(connectTimeout);
                socket.connect(new InetSocketAddress(address, port), timeout);
                socket.setSoTimeout(timeout);

                final ByteDataInput input = new SimpleByteDataInput(socket.getInputStream());
                final ByteDataOutput output = new SimpleByteDataOutput(socket.getOutputStream());

                // Start by writing the header
                final ManagementRequestHeader managementRequestHeader = new ManagementRequestHeader(ManagementProtocol.VERSION, requestId, getHandlerId());
                managementRequestHeader.write(output);
                output.flush();

                // Now read the response header
                final ManagementResponseHeader responseHeader = new ManagementResponseHeader(input);

                if (requestId != responseHeader.getResponseId()) {
                    // TODO: Exception???
                    safeClose(socket);
                    return;
                }
                // Schedule execution the operation
                Future<T> resultFuture = executorService.submit(new ExecuteTask<T>(request, responseHeader.getVersion(), socket, input, output));
                initiatingFuture.set(resultFuture);
            } catch (Throwable e) {
                safeClose(socket);
                if(requestId < connectionRetryLimit) {
                    executorService.schedule(this, connectionRetryInterval, TimeUnit.SECONDS);
                } else {
                    initiatingFuture.setException(new ManagementException("Failed to initiate request to remote domain controller", e));
                }
            }
        }
    }

    private final class InitiatingFuture<T> {
        private volatile Future<T> requestFuture;
        private volatile Exception exception;

        Future<T> get() throws ManagementException {
            boolean intr = false;
            try {
                synchronized (this) {
                    while (this.requestFuture == null && exception == null) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            intr = true;
                        }
                    }
                }
                if (exception != null) {
                    if(exception instanceof ManagementException) {
                        throw ManagementException.class.cast(exception);
                    }
                    throw new ManagementException(exception);
                }
                return requestFuture;
            } finally {
                if (intr) Thread.currentThread().interrupt();
            }
        }

        void set(final Future<T> operationFuture) {
            synchronized (this) {
                this.requestFuture = operationFuture;
                notifyAll();
            }
        }

        void setException(final Exception exception) {
            synchronized (this) {
                this.exception = exception;
                notifyAll();
            }
        }
    }

    private class ExecuteTask<T> implements Callable<T> {
        private final ManagementRequest<T> request;
        private final int protocolVersion;
        private final Socket socket;
        private final ByteDataInput input;
        private final ByteDataOutput output;

        private ExecuteTask(final ManagementRequest<T> request, final int protocolVersion, final Socket socket, final ByteDataInput input, final ByteDataOutput output) {
            this.request = request;
            this.protocolVersion = protocolVersion;
            this.socket = socket;
            this.input = input;
            this.output = output;
        }

        public T call() throws Exception {
            try {
                return request.execute(protocolVersion, output, input);
            } finally {
                safeClose(socket);
            }
        }
    }

    private void safeClose(final Socket socket) {
        if (socket == null)
            return;
        try {
            socket.shutdownOutput();
        } catch (IOException ignored) {
        }
        try {
            socket.shutdownInput();
        } catch (IOException ignored) {
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
