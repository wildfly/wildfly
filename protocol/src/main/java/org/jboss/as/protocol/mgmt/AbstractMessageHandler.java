/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.protocol.ProtocolLogger;
import org.jboss.as.protocol.ProtocolMessages;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.MessageOutputStream;
import org.jboss.threads.AsyncFuture;
import org.xnio.Cancellable;

import java.io.DataInput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for request/response handling
 *
 * @author Emanuel Muckenhuber
 */
public abstract class AbstractMessageHandler<T, A> extends ActiveOperationSupport<T, A> implements ManagementMessageHandler {

    private final ExecutorService executorService;
    private final AtomicInteger requestID = new AtomicInteger();
    private final Map<Integer, ActiveRequest> requests = Collections.synchronizedMap(new HashMap<Integer, ActiveRequest>());

    protected AbstractMessageHandler(final ExecutorService executorService) {
        super(executorService);
        if(executorService == null) {
            throw ProtocolMessages.MESSAGES.nullExecutor();
        }
        this.executorService = executorService;
    }

    /**
     * Get the executor
     *
     * @return the executor
     */
    protected ExecutorService getExecutor() {
        return executorService;
    }

    /**
     * Get the request handler.
     *
     * @param operationType the operation type
     * @return the request handler
     */
    protected ManagementRequestHandler<T, A> getRequestHandler(final byte operationType) {
        return getFallbackHandler();
    }

    /**
     * Validate whether the request can be handled.
     *
     * @param header the protocol header
     * @return the management request header
     * @throws IOException
     */
    protected ManagementRequestHeader validateRequest(final ManagementProtocolHeader header) throws IOException {
        return (ManagementRequestHeader) header;
    }

    /**
     * Handle a message.
     *
     * @param channel the channel
     * @param input the message
     * @param header the management protocol header
     * @throws IOException
     */
    public void handleMessage(final Channel channel, final DataInput input, final ManagementProtocolHeader header) throws IOException {
        final byte type = header.getType();
        if(type == ManagementProtocol.TYPE_RESPONSE) {
            // Handle response to local requests
            final ManagementResponseHeader response =  (ManagementResponseHeader) header;
            final ActiveRequest request = requests.remove(response.getResponseId());
            if(request == null) {
                ProtocolLogger.CONNECTION_LOGGER.noSuchRequest(response.getResponseId(), channel);
            } else if(response.getError() != null) {
                // Actually we could move this in the response handler
                request.context.getResultHandler().failed(new IOException(response.getError()));
            } else {
                handleMessage(channel, input, header, request.context, request.handler);
            }
        } else {
            // Handle requests (or other messages)
            try {
                final ManagementRequestHeader requestHeader = validateRequest(header);
                final ActiveOperation<T, A> support = getActiveOperation(requestHeader);
                if(support == null) {
                    safeWriteErrorResponse(channel, header, ProtocolMessages.MESSAGES.responseHandlerNotFound(requestHeader.getBatchId()));
                    return;
                }
                final ManagementRequestHandler<T, A> handler = getRequestHandler(requestHeader.getOperationId());
                if(handler == null) {
                    // TODO This might also be a failure for the current active operation?
                    safeWriteErrorResponse(channel, header, ProtocolMessages.MESSAGES.responseHandlerNotFound(requestHeader.getBatchId()));
                } else {
                    handleMessage(channel, input, requestHeader, support, handler);
                }
            } catch (Exception e) {
                safeWriteErrorResponse(channel, header, e);
            }
        }
    }

    /**
     * Execute a request.
     *
     * @param request the request
     * @param channel the channel
     * @param support the request support
     * @return the future result
     */
    protected AsyncFuture<T> executeRequest(final ManagementRequest<T, A> request, final Channel channel, final ActiveOperation<T, A> support) {
        assert support != null;
        final Integer requestId = this.requestID.incrementAndGet();
        final ActiveRequest ar = new ActiveRequest(support, request);
        requests.put(requestId, ar);
        final ManagementRequestHeader header = new ManagementRequestHeader(ManagementProtocol.VERSION, requestId, support.getOperationId(), request.getOperationType());
        final ActiveOperation.ResultHandler<T> resultHandler = support.getResultHandler();
        try {
            request.sendRequest(resultHandler, new ManagementRequestContext<A>() {

                @Override
                public Integer getOperationId() {
                    return support.getOperationId();
                }

                @Override
                public A getAttachment() {
                    return support.getAttachment();
                }

                @Override
                public Channel getChannel() {
                    return channel;
                }

                @Override
                public ManagementProtocolHeader getRequestHeader() {
                    return header;
                }

                private ExecutorService getExecutor() {
                    return executorService;
                }

                @Override
                public void executeAsync(final AsyncTask<A> task) {
                    final ManagementRequestContext<A> context = this;
                    final AsyncTaskRunner runner = new AsyncTaskRunner() {
                        @Override
                        protected void doExecute() {
                            try {
                                task.execute(context);
                            } catch (Exception e) {
                                resultHandler.failed(e);
                                requests.remove(requestId);
                            }
                        }
                    };
                    support.addCancellable(runner);
                    getExecutor().execute(runner);
                }

                @Override
                public FlushableDataOutput writeMessage(final ManagementProtocolHeader header) throws IOException {
                    final MessageOutputStream os = channel.writeMessage();
                    return writeHeader(header, os);
                }
            });

            channel.addCloseHandler(new CloseHandler<Channel>() {
                @Override
                public void handleClose(Channel closed, IOException e) {
                    if (channel == closed) {
                        IOException failure = e == null ? new IOException("Channel closed") : e;
                        resultHandler.failed(failure);
                    }
                }
            });

        } catch (Exception e) {
            resultHandler.failed(e);
        }
        return support.getResult();
    }

    /**
     * Handle a message.
     *
     * @param channel the channel
     * @param message the message
     * @param header the protocol header
     * @param support the request support
     * @param handler the request handler
     */
    protected void handleMessage(final Channel channel, final DataInput message, final ManagementProtocolHeader header,
                                 final ActiveOperation<T, A> support, final ManagementRequestHandler<T, A> handler) {
        assert support != null;
        final ActiveOperation.ResultHandler<T> resultHandler = support.getResultHandler();
        try {
            handler.handleRequest(message, resultHandler, new ManagementRequestContext<A>() {

                @Override
                public Integer getOperationId() {
                    return support.getOperationId();
                }

                @Override
                public A getAttachment() {
                    return support.getAttachment();
                }

                @Override
                public Channel getChannel() {
                    return channel;
                }

                @Override
                public ManagementProtocolHeader getRequestHeader() {
                    return header;
                }

                private ExecutorService getExecutor() {
                    return executorService;
                }

                @Override
                public void executeAsync(final AsyncTask<A> task) {
                    final ManagementRequestContext<A> context = this;
                    final AsyncTaskRunner runner = new AsyncTaskRunner() {
                        @Override
                        protected void doExecute() {
                            try {
                                task.execute(context);
                            } catch (Exception e) {
                                ProtocolLogger.ROOT_LOGGER.errorf(e, " failed to process async request for %s on channel %s", task, channel);
                                if(resultHandler.failed(e)) {
                                    safeWriteErrorResponse(channel, header, e);
                                }
                            }
                        }
                    };
                    support.addCancellable(runner);
                    getExecutor().execute(runner);
                }

                @Override
                public FlushableDataOutput writeMessage(final ManagementProtocolHeader header) throws IOException {
                    final MessageOutputStream os = channel.writeMessage();
                    return writeHeader(header, os);
                }

            });
        } catch (Exception e) {
            resultHandler.failed(e);
            safeWriteErrorResponse(channel, header, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        super.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdownNow() {
        shutdown();
        cancelAllActiveOperations();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        return super.awaitCompletion(timeout, unit);
    }

    /**
     * Safe write error response.
     *
     * @param channel the channel
     * @param header the request header
     * @param error the exception
     */
    protected static void safeWriteErrorResponse(final Channel channel, final ManagementProtocolHeader header, final Exception error) {
        if(header.getType() == ManagementProtocol.TYPE_REQUEST) {
            try {
                writeErrorResponse(channel, (ManagementRequestHeader) header, error);
            } catch(IOException ioe) {
                ProtocolLogger.ROOT_LOGGER.tracef(ioe, "failed to write error response for %s on channel: %s", header, channel);
            }
        }
    }

    /**
     * Write an error response.
     *
     * @param channel the channel
     * @param header the request
     * @param error the error
     * @throws IOException
     */
    protected static void writeErrorResponse(final Channel channel, final ManagementRequestHeader header, final Exception error) throws IOException {
        final ManagementResponseHeader response = ManagementResponseHeader.create(header, error);
        final MessageOutputStream output = channel.writeMessage();
        try {
            writeHeader(response, output);
            output.close();
        } finally {
            StreamUtils.safeClose(output);
        }
    }

    /**
     * Write the management protocol header.
     *
     * @param header the mgmt protocol header
     * @param os the output stream
     * @throws IOException
     */
    protected static FlushableDataOutput writeHeader(final ManagementProtocolHeader header, final OutputStream os) throws IOException {
        final FlushableDataOutput output = FlushableDataOutputImpl.create(os);
        header.write(output);
        return output;
    }

    /**
     * Get a fallback handler.
     *
     * @return the fallback handler
     */
    protected ManagementRequestHandler<T, A> getFallbackHandler() {
        return new ManagementRequestHandler<T, A>() {
            @Override
            public void handleRequest(final DataInput input, ActiveOperation.ResultHandler<T> resultHandler, ManagementRequestContext<A> context) throws IOException {
                final Exception error = new IOException("no handler registered");
                if(resultHandler.failed(error)) {
                    safeWriteErrorResponse(context.getChannel(), context.getRequestHeader(), error);
                }
            }
        };
    }

    private class ActiveRequest {

        private final ActiveOperation<T, A> context;
        private final ManagementRequestHandler<T, A> handler;

        ActiveRequest(ActiveOperation<T, A> context, ManagementRequestHandler<T, A> handler) {
            this.context = context;
            this.handler = handler;
        }
    }

    private abstract static class AsyncTaskRunner implements Runnable, Cancellable {

        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile Thread thread;

        @Override
        public Cancellable cancel() {
            if(cancelled.compareAndSet(false, true)) {
                final Thread thread = this.thread;
                if(thread != null) {
                    thread.interrupt();
                }
            }
            return this;
        }

        /**
         * Execute...
         */
        protected abstract void doExecute();

        @Override
        public void run() {
            if(cancelled.get()) {
                return;
            }
            this.thread = Thread.currentThread();
            try {
                doExecute();
            } finally {
                this.thread = null;
            }
        }
    }

}
