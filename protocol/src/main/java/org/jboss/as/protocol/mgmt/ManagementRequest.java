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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.protocol.ProtocolChannel;
import org.jboss.threads.AsyncFuture;
import org.jboss.threads.AsyncFutureTask;

/**
 * Base management request used for remote requests.  Provides the basic mechanism for connecting to a remote host controller
 * for performing a task.  It will manage connecting and retrieving the correct response.
 *
 *
 * @author John Bailey
 * @author Kabir Khan
 */
public abstract class ManagementRequest<T> extends ManagementResponseHandler<T> {
    private static final AtomicInteger requestId = new AtomicInteger();
    private final int currentRequestId = requestId.incrementAndGet();
    private final ManagementFuture<T> future = new ManagementFuture<T>();
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
     *  @param executionId the id of the 'execution' this request is a part of
     */
    protected ManagementRequest(int batchId) {
        this.batchId = batchId;
    }

    /**
     * Get the id of the protocol request. The {@link ManagementOperationHandler} will use this to
     * determine the {@link ManagementRequestHandler} to use.
     *
     * @return the request code
     */
    protected abstract byte getRequestCode();

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
     * @param channel The channel strategy
     * @return A future to retrieve the result when the request is complete
     */
    public AsyncFuture<T> execute(final ExecutorService executor, final ManagementClientChannelStrategy channelStrategy) {
        executor.execute(new Runnable() {

            @Override
            public void run() {
                final ManagementChannel channel = channelStrategy.getChannel();

                try {
                    System.out.println("Executing " + ManagementRequest.this + " " + currentRequestId + "(" + batchId + ") - 0x" + Integer.toHexString(getRequestCode()));
                    //Ends up in writeRequest(ProtocolChannel, FlushableDataOutput)
                    channel.executeRequest(ManagementRequest.this, new DelegatingResponseHandler(channelStrategy));
                } catch (Exception e) {
                    e.printStackTrace();
                    future.failed(e);
                }
            }
        });

        return future;
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

    private final class DelegatingResponseHandler extends ManagementResponseHandler<T>{
        private final ManagementClientChannelStrategy clientChannelStrategy;

        public DelegatingResponseHandler(ManagementClientChannelStrategy clientChannelStrategy) {
            this.clientChannelStrategy = clientChannelStrategy;
        }

        @Override
        protected T readResponse(DataInput input) {
            final String error = getContext().getResponse().getError();
            if (error != null) {
                future.failed(new IOException("A problem happened executing on the server: " + error));
                return null;
            }

            T result = null;
            try {
                result = ManagementRequest.this.readResponse(input);
                future.done(result);
                return result;
            } catch (Exception e) {
                future.failed(e);
            } finally {
                clientChannelStrategy.requestDone();
            }
            return result;
        }
    }

    private static class ManagementFuture<T> extends AsyncFutureTask<T>{
        protected ManagementFuture() {
            super(null);
        }

        void done(T result) {
            super.setResult(result);
        }

        void failed(Exception ex) {
            super.setFailed(ex);
        }
    }
}
