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

package org.jboss.as.protocol.mgmt;

import static org.jboss.as.protocol.ProtocolUtils.expectHeader;
import static org.jboss.as.protocol.StreamUtils.safeClose;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.SimpleByteDataInput;
import org.jboss.as.protocol.SimpleByteDataOutput;
import org.jboss.as.protocol.StreamUtils;

/**
 * Base management request used for remote requests.  Provides the basic mechanism for connecting to a remote host controller
 * for performing a task.  It will manage connecting and retrieving the correct response.
 *
 * @author John Bailey
 */
public abstract class ManagementRequest<T> extends AbstractMessageHandler {
    private int requestId = 0;
    private final ResponseFuture<T> future = new ResponseFuture<T>();
    private ManagementRequestConnectionStrategy connectionStrategy;
    // @GuardedBy(resultLock)
    private T result;
    /**
     * Ensures the assignment of 'result' by thread running responseBodyHandler
     * is visible to thread running responseEndHandler.
     */
    private final Object resultLock = new Object();

    /**
     * Get the handler id of the request.  These should match the id of a @{link org.jboss.as.host.controller.management.ManagementOperationHandler}.
     *
     * @return The handler id
     */
    protected abstract byte getHandlerId();

    /**
     * Execute the request by connecting and then delegating to the implementation's execute
     * and return a future used to get the response when complete.
     *
     * @param connectionStrategy The connection strategy
     * @return A future to retrieve the result when the request is complete
     * @throws IOException if any problems occur
     */
    public Future<T> execute(final ManagementRequestConnectionStrategy connectionStrategy) throws IOException {
        this.connectionStrategy = connectionStrategy;
        OutputStream dataOutput = null;
        ByteDataOutput output = null;
        try {
            final Connection connection = connectionStrategy.getConnection();
            connection.backupMessageHandler();

            connection.setMessageHandler(initiatingMessageHandler);
            dataOutput = connection.writeMessage();
            output = new SimpleByteDataOutput(dataOutput);
            // Start by writing the header
            final ManagementRequestHeader managementRequestHeader = new ManagementRequestHeader(ManagementProtocol.VERSION, requestId, getHandlerId());
            managementRequestHeader.write(output);
            connection.setMessageHandler(initiatingMessageHandler);
            output.close();
            dataOutput.close();
        } finally {
            safeClose(output);
            safeClose(dataOutput);
        }
        return future;
    }

    /**
     * Execute the request and wait for the result.
     *
     * @param connectionStrategy The connection strategy
     * @return The result
     * @throws IOException If any problems occur
     */
    public T executeForResult(final ManagementRequestConnectionStrategy connectionStrategy) throws Exception {
        return execute(connectionStrategy).get();
    }

    /** {@inheritDoc} */
    @Override
    public void handle(Connection connection, InputStream input) throws IOException {
        try {
            connection.setMessageHandler(responseBodyHandler);
            expectHeader(input, ManagementProtocol.RESPONSE_START);
            byte responseCode = StreamUtils.readByte(input);
            if (responseCode != getResponseCode()) {
                throw new IOException("Invalid response code.  Expecting '" + getResponseCode() + "' received '" + responseCode + "'");
            }
        } catch (Exception e) {
            future.setException(e);
        }
    }

    private MessageHandler initiatingMessageHandler = new AbstractMessageHandler() {
        @Override
        public final void handle(final Connection connection, final InputStream inputStream) throws IOException {
            final ManagementResponseHeader responseHeader;
            ByteDataInput input = null;
            try {
                input = new SimpleByteDataInput(inputStream);
                responseHeader = new ManagementResponseHeader(input);
                if (requestId != responseHeader.getResponseId()) {
                    throw new IOException("Invalid request ID expecting " + requestId + " received " + responseHeader.getResponseId());
                }
                connection.setMessageHandler(ManagementRequest.this);
                sendRequest(responseHeader.getVersion(), connection);
            } catch (Exception e) {
                future.setException(e);
            } finally {
                safeClose(input);
                if (future.isDone()) {
                    // We must have failed above and set the exception.
                    // If we fail sending we shouldn't expect a response,
                    // so consider the connection complete
                    connectionStrategy.complete();
                }
            }
        }
    };

    /**
     * Execute the request body.  This is run after the connection is established and the headers are exchanged.
     *
     * @param protocolVersion The active protocol version for the request
     * @param connection      The connection
     * @throws IOException If any errors occur
     */
    protected void sendRequest(final int protocolVersion, final Connection connection) throws IOException {
        OutputStream outputStream = null;
        ByteDataOutput output = null;
        try {
            outputStream = connection.writeMessage();
            output = new SimpleByteDataOutput(outputStream);
            output.writeByte(ManagementProtocol.REQUEST_OPERATION);
            output.writeByte(getRequestCode());
            output.writeByte(ManagementProtocol.REQUEST_START);
            output.close();
            outputStream.close();
        } finally {
            safeClose(output);
            safeClose(outputStream);
        }

        try {
            outputStream = connection.writeMessage();
            outputStream.write(ManagementProtocol.REQUEST_BODY);
            sendRequest(protocolVersion, outputStream);
            outputStream.close();
        } finally {
            safeClose(outputStream);
        }

        try {
            outputStream = connection.writeMessage();
            output = new SimpleByteDataOutput(outputStream);
            output.writeByte(ManagementProtocol.REQUEST_END);
            output.close();
            outputStream.close();
        } finally {
            safeClose(output);
            safeClose(outputStream);
        }
    }

    protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
    }

    protected abstract byte getRequestCode();

    protected abstract byte getResponseCode();

    private MessageHandler responseBodyHandler = new AbstractMessageHandler() {
        @Override
        public final void handle(final Connection connection, final InputStream input) throws IOException {
            try {
                connection.setMessageHandler(responseEndHandler);
                expectHeader(input, ManagementProtocol.RESPONSE_BODY);
                synchronized (resultLock) {
                    result = receiveResponse(input);
                }
            }
            catch (Exception e) {
                future.setException(e);
            }
        }
    };

    private MessageHandler responseEndHandler = new AbstractMessageHandler() {
        @Override
        public final void handle(final Connection connection, final InputStream input) throws IOException {
            try {
                connection.restoreMessageHandler();
                expectHeader(input, ManagementProtocol.RESPONSE_END);
                synchronized (resultLock) {
                    future.set(result);
                }
            }
            catch (Exception e) {
                future.setException(e);
            }
            finally {
                connectionStrategy.complete();
            }
        }
    };

    protected T receiveResponse(final InputStream input) throws IOException {
        return null;
    }

    private final class ResponseFuture<R> implements Future<R>{
        private volatile R result;
        private volatile Exception exception;
        private AtomicBoolean valueSet = new AtomicBoolean();

        public R get() throws InterruptedException, ExecutionException {
            boolean intr = false;
            try {
                synchronized (this) {
                    while (!valueSet.get()) {
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


        void set(final R result) {
            synchronized (this) {
                if(valueSet.compareAndSet(false, true)) {
                    this.result = result;
                    notifyAll();
                }
            }
        }

        void setException(final Exception exception) {
            synchronized (this) {
                if(valueSet.compareAndSet(false, true)) {
                    this.exception = exception;
                    notifyAll();
                }
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

        public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }
}
