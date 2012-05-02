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

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelAssociation;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestHandlerFactory;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ManagementResponseHeader;
import static org.jboss.as.protocol.mgmt.ProtocolUtils.expectHeader;
import org.jboss.dmr.ModelNode;
import org.jboss.threads.AsyncFuture;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractModelControllerClient implements ModelControllerClient, ManagementRequestHandlerFactory {

    private static ManagementRequestHandler<ModelNode, OperationExecutionContext> MESSAGE_HANDLER = new HandleReportRequestHandler();
    private static ManagementRequestHandler<ModelNode, OperationExecutionContext> GET_INPUT_STREAM = new ReadAttachmentInputStreamRequestHandler();

    private static final OperationMessageHandler NO_OP_HANDLER = OperationMessageHandler.DISCARD;

    /**
     * Get the mgmt channel association.
     *
     * @return the channel association
     * @throws IOException
     */
    protected abstract ManagementChannelAssociation getChannelAssociation() throws IOException;

    @Override
    public ModelNode execute(final ModelNode operation) throws IOException {
        return executeForResult(OperationExecutionContext.create(operation));
    }

    @Override
    public ModelNode execute(final Operation operation) throws IOException {
        return executeForResult(OperationExecutionContext.create(operation));
    }

    @Override
    public ModelNode execute(final ModelNode operation, final OperationMessageHandler messageHandler) throws IOException {
        return executeForResult(OperationExecutionContext.create(operation, messageHandler));
    }

    @Override
    public ModelNode execute(Operation operation, OperationMessageHandler messageHandler) throws IOException {
        return executeForResult(OperationExecutionContext.create(operation, messageHandler));
    }

    @Override
    public AsyncFuture<ModelNode> executeAsync(final ModelNode operation, final OperationMessageHandler messageHandler) {
        try {
            return execute(OperationExecutionContext.create(operation, messageHandler));
        } catch (IOException e)  {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AsyncFuture<ModelNode> executeAsync(final Operation operation, final OperationMessageHandler messageHandler) {
        try {
            return execute(OperationExecutionContext.create(operation, messageHandler));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ManagementRequestHandler<?, ?> resolveHandler(RequestHandlerChain handlers, ManagementRequestHeader header) {
        final byte operationType = header.getOperationId();
        if (operationType == ModelControllerProtocol.HANDLE_REPORT_REQUEST) {
            return MESSAGE_HANDLER;
        } else if (operationType == ModelControllerProtocol.GET_INPUTSTREAM_REQUEST) {
            return GET_INPUT_STREAM;
        }
        return handlers.resolveNext();
    }

    /**
     * Execute for result.
     *
     * @param executionContext the execution context
     * @return the result
     * @throws IOException for any error
     */
    private ModelNode executeForResult(final OperationExecutionContext executionContext) throws IOException {
        try {
            return execute(executionContext).get();
        } catch(Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Execute a request.
     *
     * @param executionContext the execution context
     * @return the future result
     * @throws IOException
     */
    private AsyncFuture<ModelNode> execute(final OperationExecutionContext executionContext) throws IOException {
        return executeRequest(new AbstractManagementRequest<ModelNode, OperationExecutionContext>() {

            @Override
            public byte getOperationType() {
                return ModelControllerProtocol.EXECUTE_ASYNC_CLIENT_REQUEST;
            }

            @Override
            protected void sendRequest(final ActiveOperation.ResultHandler<ModelNode> resultHandler,
                                       final ManagementRequestContext<OperationExecutionContext> context,
                                       final FlushableDataOutput output) throws IOException {
                // Write the operation
                final List<InputStream> streams = executionContext.operation.getInputStreams();
                final ModelNode operation = executionContext.operation.getOperation();
                int inputStreamLength = 0;
                if (streams != null) {
                    inputStreamLength = streams.size();
                }
                output.write(ModelControllerProtocol.PARAM_OPERATION);
                operation.writeExternal(output);
                output.write(ModelControllerProtocol.PARAM_INPUTSTREAMS_LENGTH);
                output.writeInt(inputStreamLength);
            }

            @Override
            public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<ModelNode> resultHandler, final ManagementRequestContext<OperationExecutionContext> context) throws IOException {
                expectHeader(input, ModelControllerProtocol.PARAM_RESPONSE);
                final ModelNode node = new ModelNode();
                node.readExternal(input);
                resultHandler.done(node);
                expectHeader(input, ManagementProtocol.RESPONSE_END);
            }
        }, executionContext);
    }

    private static class ReadAttachmentInputStreamRequestHandler implements ManagementRequestHandler<ModelNode, OperationExecutionContext> {

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<ModelNode> resultHandler,
                              final ManagementRequestContext<OperationExecutionContext> context) throws IOException {
            // Read the inputStream index
            expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAM_INDEX);
            final int index = input.readInt();
            context.executeAsync(new ManagementRequestContext.AsyncTask<OperationExecutionContext>() {
                @Override
                public void execute(final ManagementRequestContext<OperationExecutionContext> context) throws Exception {
                    final OperationExecutionContext exec = context.getAttachment();
                    final ManagementRequestHeader header = ManagementRequestHeader.class.cast(context.getRequestHeader());
                    final ManagementResponseHeader response = new ManagementResponseHeader(header.getVersion(), header.getRequestId(), null);
                    final InputStreamEntry entry = exec.getStream(index);
                    synchronized (entry) {
                        // Initialize the stream entry
                        final int size = entry.initialize();
                        try {
                            final FlushableDataOutput output = context.writeMessage(response);
                            try {
                                output.writeByte(ModelControllerProtocol.PARAM_INPUTSTREAM_LENGTH);
                                output.writeInt(size);
                                output.writeByte(ModelControllerProtocol.PARAM_INPUTSTREAM_CONTENTS);
                                entry.copyStream(output);
                                output.writeByte(ManagementProtocol.RESPONSE_END);
                                output.close();
                            } finally {
                                StreamUtils.safeClose(output);
                            }
                        } finally {
                            // the caller is responsible for closing the input streams
                            // StreamUtils.safeClose(is);
                        }
                    }
                }
            });
        }

    }

    private static class HandleReportRequestHandler implements ManagementRequestHandler<ModelNode, OperationExecutionContext> {

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<ModelNode> resultHandler, final ManagementRequestContext<OperationExecutionContext> context) throws IOException {
            expectHeader(input, ModelControllerProtocol.PARAM_MESSAGE_SEVERITY);
            final MessageSeverity severity = Enum.valueOf(MessageSeverity.class, input.readUTF());
            expectHeader(input, ModelControllerProtocol.PARAM_MESSAGE);
            final String message = input.readUTF();
            expectHeader(input, ManagementProtocol.REQUEST_END);

            final OperationExecutionContext requestContext = context.getAttachment();
            // perhaps execute async
            final OperationMessageHandler handler = requestContext.getOperationMessageHandler();
            handler.handleReport(severity, message);
        }

    }

    protected AsyncFuture<ModelNode> executeRequest(final ManagementRequest<ModelNode, OperationExecutionContext> request, final OperationExecutionContext attachment) throws IOException {
        final ActiveOperation<ModelNode, OperationExecutionContext> support = getChannelAssociation().executeRequest(request, attachment);
        return new DelegatingCancellableAsyncFuture(support.getResult(), support.getOperationId());
    }

    static class OperationExecutionContext implements ActiveOperation.CompletedCallback<ModelNode> {

        private final Operation operation;
        private final OperationMessageHandler handler;
        private final List<InputStreamEntry> streams;

        OperationExecutionContext(final Operation operation, final OperationMessageHandler handler) {
            this.operation = operation;
            this.handler = handler != null ? handler : NO_OP_HANDLER;
            this.streams = createStreamEntries(operation);
        }

        OperationMessageHandler getOperationMessageHandler() {
            return handler;
        }

        InputStreamEntry getStream(int index) {
            final InputStreamEntry entry = streams.get(index);
            if(entry == null) {
                // Hmm, a null input-stream should be a failure?
                return InputStreamEntry.EMPTY;
            }
            return entry;
        }

        @Override
        public void completed(ModelNode result) {
            closeAttachments();
        }

        @Override
        public void failed(Exception e) {
            closeAttachments();
        }

        @Override
        public void cancelled() {
            closeAttachments();
        }

        private void closeAttachments() {
            for(final InputStreamEntry entry : streams) {
                StreamUtils.safeClose(entry);
            }
            if(operation.isAutoCloseStreams()) {
                StreamUtils.safeClose(operation);
            }
        }

        static OperationExecutionContext create(final ModelNode operation) {
            return create(new OperationBuilder(operation).build(), NO_OP_HANDLER);
        }

        static OperationExecutionContext create(final Operation operation) {
            return create(operation, NO_OP_HANDLER);
        }

        static OperationExecutionContext create(final ModelNode operation, final OperationMessageHandler handler) {
            return create(new OperationBuilder(operation).build(), handler);
        }

        static OperationExecutionContext create(final Operation operation, final OperationMessageHandler handler) {
            return new OperationExecutionContext(operation, handler);
        }

    }

    /**
     * Wraps the request execution AsyncFuture in an AsyncFuture impl that handles cancellation by sending a cancellation
     * request to the remote side.
     */
    private class DelegatingCancellableAsyncFuture extends AbstractDelegatingAsyncFuture<ModelNode> {

        private final int batchId;
        private DelegatingCancellableAsyncFuture(final AsyncFuture<ModelNode> delegate, final int batchId) {
            super(delegate);
            this.batchId = batchId;
        }

        @Override
        public void asyncCancel(boolean interruptionDesired) {
            try {
                getChannelAssociation().executeRequest(batchId, new CancelAsyncRequest());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Request cancelling the remote operation.
     */
    private static class CancelAsyncRequest extends AbstractManagementRequest<ModelNode, OperationExecutionContext> {

        @Override
        public byte getOperationType() {
            return ModelControllerProtocol.CANCEL_ASYNC_REQUEST;
        }

        @Override
        protected void sendRequest(ActiveOperation.ResultHandler<ModelNode> resultHandler, ManagementRequestContext<OperationExecutionContext> context, FlushableDataOutput output) throws IOException {
            //
        }

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<ModelNode> resultHandler, ManagementRequestContext<OperationExecutionContext> context) throws IOException {
            // Once the remote operation returns, we can set the cancelled status
            resultHandler.cancel();
        }
    }

    static List<InputStreamEntry> createStreamEntries(final Operation operation) {
        final List<InputStream> streams = operation.getInputStreams();
        if(streams.isEmpty()) {
            return Collections.emptyList();
        }
        final List<InputStreamEntry> entries = new ArrayList<InputStreamEntry>();
        final boolean autoClose = operation.isAutoCloseStreams();
        for(final InputStream stream : streams) {
            if(stream instanceof InputStreamEntry) {
                entries.add((InputStreamEntry) stream);
            } else {
                // TODO don't copy everything to memory... perhaps use InputStreamEntry.CachingStreamEntry
                entries.add(new InputStreamEntry.InMemoryEntry(stream, autoClose));
            }
        }
        return entries;
    }

}
