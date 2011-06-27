/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.client.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementBatchIdManager;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.ManagementResponseHandler;
import org.jboss.as.protocol.mgmt.RequestProcessingException;
import org.jboss.as.protocol.old.ProtocolUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.threads.AsyncFuture;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractModelControllerClient implements ModelControllerClient, ManagementOperationHandler {
    private final Map<Integer, ExecuteRequestContext> activeRequests = Collections.synchronizedMap(new HashMap<Integer, ExecuteRequestContext>());
    protected final ExecutorService executor = Executors.newCachedThreadPool();
    final Logger log = Logger.getLogger("org.jboss.as.controller.client");

    @Override
    public void close() throws IOException {
        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdownNow();
    }

    @Override
    public ModelNode execute(ModelNode operation) throws IOException {
        return execute(operation, null);
    }

    @Override
    public ModelNode execute(Operation operation) throws IOException {
        return execute(operation, null);
    }

    @Override
    public ModelNode execute(ModelNode operation, OperationMessageHandler messageHandler) throws IOException {
        return executeSynch(operation, null, messageHandler);
    }

    @Override
    public ModelNode execute(Operation operation, OperationMessageHandler messageHandler) throws IOException {
        return executeSynch(operation.getOperation(), operation, messageHandler);
    }

    @Override
    public AsyncFuture<ModelNode> executeAsync(ModelNode operation, OperationMessageHandler messageHandler) {
        return executeAsync(operation, null, messageHandler);
    }

    @Override
    public AsyncFuture<ModelNode> executeAsync(Operation operation, OperationMessageHandler messageHandler) {
        return executeAsync(operation.getOperation(), operation, messageHandler);
    }

    /** {@inheritDoc} */
    @Override
    public ManagementRequestHandler getRequestHandler(final byte id) {
        if (id == ModelControllerProtocol.HANDLE_REPORT_REQUEST) {
            return new HandleReportRequestHandler();
        } else if (id == ModelControllerProtocol.GET_INPUTSTREAM_REQUEST) {
            return new ReadAttachmentInputStreamRequestHandler();
        }
        return null;
    }

    protected abstract ManagementClientChannelStrategy getClientChannelStrategy() throws URISyntaxException, IOException;

    private ModelNode executeSynch(ModelNode operation, OperationAttachments attachments, OperationMessageHandler messageHandler) throws IOException {
        final int batchId = ManagementBatchIdManager.DEFAULT.createBatchId();

        try {
            return new ExecuteRequest(batchId, false, operation, messageHandler, attachments).executeForResult(executor, getClientChannelStrategy());
        } catch (Exception e) {
            ManagementBatchIdManager.DEFAULT.freeBatchId(batchId);
            Throwable cause = e;
            if (e instanceof ExecutionException) {
                cause = e.getCause();
            }
            if (cause instanceof IOException) {
                throw (IOException)cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            }
            throw new IOException(cause);
        }
    }

    private AsyncFuture<ModelNode> executeAsync(ModelNode operation, OperationAttachments attachments, OperationMessageHandler messageHandler) {
        final int batchId = ManagementBatchIdManager.DEFAULT.createBatchId();
        try {
            return new DelegatingCancellableAsyncFuture(new ExecuteRequest(batchId, true, operation, messageHandler, attachments).execute(executor, getClientChannelStrategy()), batchId);
        } catch (Exception e) {
            ManagementBatchIdManager.DEFAULT.freeBatchId(batchId);
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Propagates an execute() call from this proxy controller to the remote target controller
     */
    private class ExecuteRequest extends ManagementRequest<ModelNode> {

        private final ExecuteRequestContext executeRequestContext;
        private final ModelNode operation;
        private final boolean async;

        ExecuteRequest(final int batchId, final boolean async, final ModelNode operation, final OperationMessageHandler messageHandler, final OperationAttachments attachments) {
            super(batchId);
            this.operation = operation;
            this.async = async;
            executeRequestContext = new ExecuteRequestContext(this, messageHandler, attachments);
        }

        @Override
        protected byte getRequestCode() {
            return async ? ModelControllerProtocol.EXECUTE_ASYNC_CLIENT_REQUEST : ModelControllerProtocol.EXECUTE_CLIENT_REQUEST;
        }

        protected CloseHandler<Channel> getRequestCloseHandler(){
            return executeRequestContext.getRequestCloseHandler();
        }

        @Override
        protected void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
            try {
                log.tracef("Client writing request %d", getBatchId());
                activeRequests.put(getBatchId(), executeRequestContext);

                output.write(ModelControllerProtocol.PARAM_OPERATION);
                operation.writeExternal(output);
                output.write(ModelControllerProtocol.PARAM_INPUTSTREAMS_LENGTH);
                int inputStreamLength = 0;
                if (executeRequestContext.getAttachments() != null) {
                    List<InputStream> streams = executeRequestContext.getAttachments().getInputStreams();
                    if (streams != null) {
                        inputStreamLength = streams.size();
                    }
                }
                output.writeInt(inputStreamLength);
                log.tracef("Client wrote request %d successfully", getBatchId());
            } catch (Exception e) {
                log.tracef(e, "Client wrote request %d with error", getBatchId());
                setError(e);
                if (e instanceof IOException) {
                    throw (IOException)e;
                }
                if (e instanceof RuntimeException) {
                    throw (RuntimeException)e;
                }
                throw new IOException(e);
            }
        }

        protected ManagementResponseHandler<ModelNode> getResponseHandler() {
            return new ManagementResponseHandler<ModelNode>() {
                @Override
                protected ModelNode readResponse(final DataInput input) throws IOException {
                    log.tracef("Client reading response %d", getBatchId());
                    try {
                        ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_RESPONSE);
                        ModelNode node = new ModelNode();
                        node.readExternal(input);
                        log.tracef("Client read response %d successfully", getBatchId());
                        return node;
                    } catch (Exception e) {
                        log.tracef(e, "Client read response %d with error", getBatchId());
                        //super.setError(new ClientException(e));
                        setError(e);
                        if (e instanceof IOException) {
                            throw (IOException)e;
                        }
                        if (e instanceof RuntimeException) {
                            throw (RuntimeException)e;
                        }
                        throw new IOException(e);
                    } finally {
                        ManagementBatchIdManager.DEFAULT.freeBatchId(getBatchId());
                        activeRequests.remove(getCurrentRequestId());
                        executeRequestContext.done();
                    }
                }
            };
        }

        @Override
        protected void setError(final Exception e) {
            super.setError(e instanceof IOException ? e : new IOException(e));
        }
    }

    /**
     * Handles {@link OperationMessageHandler#handleReport(org.jboss.as.controller.client.MessageSeverity, String)} calls
     * done in the remote target controller
     */
    private class HandleReportRequestHandler extends ManagementRequestHandler {

        @Override
        protected void readRequest(final DataInput input) throws IOException {
            int batchId = getHeader().getBatchId();
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_MESSAGE_SEVERITY);
            MessageSeverity severity = Enum.valueOf(MessageSeverity.class, input.readUTF());
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_MESSAGE);
            String message = input.readUTF();

            ExecuteRequestContext requestContext = activeRequests.get(batchId);
            if (requestContext == null) {
                throw new IOException("No active request found for " + batchId);
            }
            if (requestContext.getMessageHandler() != null) {
                requestContext.getMessageHandler().handleReport(severity, message);
            }
        }

        protected void processRequest() {

        }
    }

    /**
     * Handles reads on the inputstreams returned by {@link OperationAttachments#getInputStreams()}
     * done in the remote target controller
     */
    private class ReadAttachmentInputStreamRequestHandler extends ManagementRequestHandler {
        InputStream attachmentInput;
        byte[] bytes;
        @Override
        protected void readRequest(final DataInput input) throws IOException {
            int batchId = getHeader().getBatchId();
            log.tracef("Client got inputstream request %d",  batchId);
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAM_INDEX);
            int index = input.readInt();

            ExecuteRequestContext requestContext = activeRequests.get(batchId);
            if (requestContext == null) {
                throw new IOException("No active request found for " + batchId);
            }
            InputStream in = requestContext.getAttachments().getInputStreams().get(index);
            attachmentInput = in != null ? new BufferedInputStream(in) : null;
        }

        protected void processRequest() throws RequestProcessingException {
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                if (attachmentInput != null) {
                    int i = attachmentInput.read();
                    while (i != -1) {
                        bout.write(i);
                        i = attachmentInput.read();
                    }
                }
                bytes = bout.toByteArray();
            } catch (IOException e) {
                throw new RequestProcessingException(e);
            } finally {
                //Think the caller is responsible for closing these
                //IoUtils.safeClose(attachmentInput);
            }
        }


        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            output.write(ModelControllerProtocol.PARAM_INPUTSTREAM_LENGTH);
            output.writeInt(bytes.length);
            output.write(ModelControllerProtocol.PARAM_INPUTSTREAM_CONTENTS);
            output.write(bytes);
            log.tracef("Client handled inputstream request %d",  getHeader().getBatchId());
        }
    }

    private static class ExecuteRequestContext {
        final ExecuteRequest executeRequest;
        final OperationMessageHandler messageHandler;
        final OperationAttachments attachments;
        volatile boolean done;

        ExecuteRequestContext(final ExecuteRequest executeRequest, final OperationMessageHandler messageHandler, final OperationAttachments attachments) {
            this.executeRequest = executeRequest;
            this.messageHandler = messageHandler;
            this.attachments = attachments;
        }

        CloseHandler<Channel> getRequestCloseHandler(){
            return new CloseHandler<Channel>() {
                public void handleClose(final Channel closed, final IOException exception) {
                    if (!done) {
                        executeRequest.setError(new IOException("Channel closed"));
                    }
                }
            };
        }

        OperationMessageHandler getMessageHandler() {
            return messageHandler;
        }

        OperationAttachments getAttachments() {
            return attachments;
        }

        void done() {
            this.done = true;
        }
    }

    private class DelegatingCancellableAsyncFuture implements AsyncFuture<ModelNode>{
        private final AsyncFuture<ModelNode> delegate;
        private final int batchId;
        private volatile boolean isCancelled;

        public DelegatingCancellableAsyncFuture(AsyncFuture<ModelNode> delegate, int batchId) {
            this.delegate = delegate;
            this.batchId = batchId;
        }

        public org.jboss.threads.AsyncFuture.Status await() throws InterruptedException {
            return delegate.await();
        }

        public org.jboss.threads.AsyncFuture.Status await(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.await(timeout, unit);
        }

        public ModelNode getUninterruptibly() throws CancellationException, ExecutionException {
            return delegate.getUninterruptibly();
        }

        public ModelNode getUninterruptibly(long timeout, TimeUnit unit) throws CancellationException, ExecutionException,
                TimeoutException {
            return delegate.getUninterruptibly(timeout, unit);
        }

        public org.jboss.threads.AsyncFuture.Status awaitUninterruptibly() {
            return delegate.awaitUninterruptibly();
        }

        public org.jboss.threads.AsyncFuture.Status awaitUninterruptibly(long timeout, TimeUnit unit) {
            return delegate.awaitUninterruptibly(timeout, unit);
        }

        public boolean isDone() {
            return delegate.isDone();
        }

        public org.jboss.threads.AsyncFuture.Status getStatus() {
            return delegate.getStatus();
        }

        public <A> void addListener(org.jboss.threads.AsyncFuture.Listener<? super ModelNode, A> listener, A attachment) {
            delegate.addListener(listener, attachment);
        }

        public ModelNode get() throws InterruptedException, ExecutionException {
            return delegate.get();
        }

        public ModelNode get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.get(timeout, unit);
        }

        @Override
        public boolean isCancelled() {
            return isCancelled;
        }

        @Override
        public boolean cancel(boolean interruptionDesired) {
            if (!activeRequests.containsKey(batchId)) {
                return false;
            }
            try {
                new CancelAsyncRequest().executeForResult(executor, getClientChannelStrategy());
                if (isDone()) {
                    isCancelled = false;
                }
                isCancelled = true;
                return isCancelled;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public void asyncCancel(boolean interruptionDesired) {
            try {
                new CancelAsyncRequest().execute(executor, getClientChannelStrategy());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        private class CancelAsyncRequest extends ManagementRequest<Void>{

            public CancelAsyncRequest() {
                super(batchId);
            }

            @Override
            protected byte getRequestCode() {
                return ModelControllerProtocol.CANCEL_ASYNC_REQUEST;
            }


            @Override
            protected ManagementResponseHandler<Void> getResponseHandler() {
                return ManagementResponseHandler.EMPTY_RESPONSE;
            }
        }
    }
}
