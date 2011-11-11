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

import java.io.DataInput;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.protocol.ProtocolChannel;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.threads.AsyncFuture;
import org.jboss.threads.AsyncFutureTask;

import static org.jboss.as.protocol.ProtocolLogger.ROOT_LOGGER;
import static org.jboss.as.protocol.ProtocolMessages.MESSAGES;

/**
 * Base management request used for remote requests.  Provides the basic mechanism for connecting to a remote host controller
 * for performing a task.  It will manage connecting and retrieving the correct response.
 *
 * @author John Bailey
 * @author Kabir Khan
 * @author Emanuel Muckenhuber
 */
public abstract class ManagementRequest<T> extends AsyncFutureTask<T> {

    private static final AtomicInteger requestId = new AtomicInteger();
    private final int currentRequestId = requestId.incrementAndGet();
    private final int batchId;

    /**
     * Create a new ManagementRequest that is not part of an 'execution', i.e. this is a standalone request.
     */
    protected ManagementRequest() {
        this(0);
    }

    /**
     * Create a new ManagementRequest that is part of a batch
     *
     *  @param batchId the id of the 'execution' this request is a part of
     */
    protected ManagementRequest(final int batchId) {
        this(batchId, null);
    }

    /**
     * Create a new ManagementRequest that is part of a batch and has an executor
     * to run listener tasks.
     *
     *  @param batchId the id of the 'execution' this request is a part of
     * @param executor the executor
     */
    protected ManagementRequest(final int batchId, Executor executor) {
        super(executor);
        this.batchId = batchId;
    }

    /**
     * Get the id of the protocol request. The {@link ManagementOperationHandler} will use this to
     * determine the {@link ManagementRequestHandler} to use.
     *
     * @return the request code
     */
    protected abstract byte getRequestCode();

    /**
     * Get the associated response handler.
     *
     * @return the response handler
     */
    protected abstract ManagementResponseHandler<T> getResponseHandler();

    protected int getCurrentRequestId() {
        return currentRequestId;
    }

    protected int getBatchId() {
        return batchId;
    }

    /**
     * Execute the request by connecting and then delegating to the implementation's execute
     * and return a future used to get the response when complete.
     *
     * @param executor The executor to use to handle the request and response
     * @param channelStrategy The channel strategy
     * @return A future to retrieve the result when the request is complete
     */
    public AsyncFuture<T> execute(final ExecutorService executor, final ManagementClientChannelStrategy channelStrategy) {
        ROOT_LOGGER.tracef("Scheduling request %s - %d (%d)", this, getBatchId(), getCurrentRequestId());
        executor.execute(new Runnable() {
            @Override
            public void run() {

                try {
                    final ManagementChannel channel = channelStrategy.getChannel();
                    ROOT_LOGGER.tracef("Got channel %s from request %s for %d", channel, ManagementRequest.this, getCurrentRequestId());

                    //Ends up in writeRequest(ProtocolChannel, FlushableDataOutput)
                    channel.executeRequest(ManagementRequest.this, new DelegatingResponseHandler(channelStrategy));
                } catch (Exception e) {
                    ROOT_LOGGER.tracef(e, "Could not get channel for request %s, for %d", ManagementRequest.this, getCurrentRequestId());
                    failed(e);
                }
            }
        });

        return this;
    }

    void writeRequest(ProtocolChannel channel, FlushableDataOutput output) throws IOException {
        //Body
        writeRequest(ManagementProtocol.VERSION, output);
        output.writeByte(ManagementProtocol.REQUEST_END);
    }

    /**
     * Execute the request and wait for the result.
     *
     * @param executor The executor to use to handle the request and response
     * @param channelStrategy The channel strategy
     * @return The result
     * @throws IOException If any problems occur
     */
    public T executeForResult(final ExecutorService executor, final ManagementClientChannelStrategy channelStrategy) throws Exception {
        return execute(executor, channelStrategy).get();
    }

    /**
     * Override to send extra parameters to the server {@link ManagementRequestHandler}. This default
     * implementation does not send any extra data
     *
     * @param protocolVersion the protocol version
     * @param output the data output to write the data to
     */
    protected void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
    }

    /**
     * Override to register a close handler for the channel that will be removed once the request is done
     *
     * @return the close handler
     */
    protected CloseHandler<Channel> getRequestCloseHandler() {
        return null;
    }

    private final class DelegatingResponseHandler extends ManagementResponseHandler<T>{
        private final ManagementClientChannelStrategy clientChannelStrategy;

        public DelegatingResponseHandler(ManagementClientChannelStrategy clientChannelStrategy) {
            this.clientChannelStrategy = clientChannelStrategy;
        }

        @Override
        protected T readResponse(DataInput input) {
            final String error = getResponseHeader().getError();
            if (error != null) {
                failed(MESSAGES.serverError(error));
                return null;
            }

            T result = null;
            try {
                ManagementResponseHandler<T> responseHandler = getResponseHandler();
                responseHandler.setContextInfo(this);
                result = responseHandler.readResponse(input);
                done(result);
                return result;
            } catch (Exception e) {
                setError(e);
            } finally {
                clientChannelStrategy.requestDone();
            }
            return result;
        }
    }

    protected void done(T result) {
        super.setResult(result);
    }

    protected void setError(Exception e) {
        super.setFailed(e);
    }

    protected boolean failed(Exception e) {
        return super.setFailed(e);
    }

}
