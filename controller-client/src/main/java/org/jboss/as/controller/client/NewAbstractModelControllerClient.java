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
package org.jboss.as.controller.client;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementBatchIdManager;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.old.ProtocolUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.threads.AsyncFuture;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
abstract class NewAbstractModelControllerClient implements NewModelControllerClient, ManagementOperationHandler {
    private final Map<Integer, ExecuteRequestContext> activeRequests = Collections.synchronizedMap(new HashMap<Integer, ExecuteRequestContext>());
    protected final ExecutorService executor = Executors.newCachedThreadPool();

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
    public ModelNode execute(NewOperation operation) throws IOException {
        return execute(operation, null);
    }

    @Override
    public ModelNode execute(ModelNode operation, OperationMessageHandler messageHandler) throws IOException {
        return executeSynch(operation, null, messageHandler);
    }

    @Override
    public ModelNode execute(NewOperation operation, OperationMessageHandler messageHandler) throws IOException {
        return executeSynch(operation.getOperation(), operation, messageHandler);
    }

    @Override
    public AsyncFuture<ModelNode> executeAsync(ModelNode operation, OperationMessageHandler messageHandler) {
        return executeAsync(operation, null, messageHandler);
    }

    @Override
    public AsyncFuture<ModelNode> executeAsync(NewOperation operation, OperationMessageHandler messageHandler) {
        return executeAsync(operation.getOperation(), operation, messageHandler);
    }

    /** {@inheritDoc} */
    @Override
    public ManagementRequestHandler getRequestHandler(final byte id) {
        if (id == NewModelControllerProtocol.HANDLE_REPORT_REQUEST) {
            return new HandleReportRequestHandler();
        } else if (id == NewModelControllerProtocol.GET_INPUTSTREAM_REQUEST) {
            return new ReadAttachmentInputStreamRequestHandler();
        }
        return null;
    }

    abstract ManagementClientChannelStrategy getClientChannelStrategy() throws URISyntaxException, IOException;

    private ModelNode executeSynch(ModelNode operation, OperationAttachments attachments, OperationMessageHandler messageHandler) {
        final int batchId = ManagementBatchIdManager.DEFAULT.createBatchId();

        try {
            return new ExecuteRequest(batchId, operation, messageHandler, attachments).executeForResult(executor, getClientChannelStrategy());
        } catch (Exception e) {
            ManagementBatchIdManager.DEFAULT.freeBatchId(batchId);
            throw new RuntimeException(e);
        }
    }

    private AsyncFuture<ModelNode> executeAsync(ModelNode operation, OperationAttachments attachments, OperationMessageHandler messageHandler){
        final int batchId = ManagementBatchIdManager.DEFAULT.createBatchId();
        try {
            return new ExecuteRequest(batchId, operation, messageHandler, attachments).execute(executor, getClientChannelStrategy());
        } catch (Exception e) {
            ManagementBatchIdManager.DEFAULT.freeBatchId(batchId);
            throw new RuntimeException(e);
        }
    }

    /**
     * Propagates an execute() call from this proxy controller to the remote target controller
     */
    private class ExecuteRequest extends ManagementRequest<ModelNode> {

        private final ExecuteRequestContext executeRequestContext;
        private final ModelNode operation;

        ExecuteRequest(final int batchId, final ModelNode operation, final OperationMessageHandler messageHandler, final OperationAttachments attachments) {
            super(batchId);
            this.operation = operation;
            executeRequestContext = new ExecuteRequestContext(messageHandler, attachments);
        }

        @Override
        protected byte getRequestCode() {
            return NewModelControllerProtocol.EXECUTE_CLIENT_REQUEST;
        }

        @Override
        protected void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
            //TODO Cleanup: this could leak if something goes wrong in the calling code
            activeRequests.put(getBatchId(), executeRequestContext);

            output.write(NewModelControllerProtocol.PARAM_OPERATION);
            operation.writeExternal(output);
            output.write(NewModelControllerProtocol.PARAM_INPUTSTREAMS_LENGTH);
            int inputStreamLength = 0;
            if (executeRequestContext.getAttachments() != null) {
                List<InputStream> streams = executeRequestContext.getAttachments().getInputStreams();
                if (streams != null) {
                    inputStreamLength = streams.size();
                }
            }
            output.writeInt(inputStreamLength);
        }

        @Override
        protected ModelNode readResponse(final DataInput input) throws IOException {
            try {
                ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_RESPONSE);
                ModelNode node = new ModelNode();
                node.readExternal(input);
                return node;
            } finally {
                ManagementBatchIdManager.DEFAULT.freeBatchId(getBatchId());
                activeRequests.remove(getCurrentRequestId());
            }
        }
    }

    /**
     * Handles {@link OperationMessageHandler#handleReport(org.jboss.as.controller.client.MessageSeverity, String)} calls
     * done in the remote target controller
     */
    private class HandleReportRequestHandler extends ManagementRequestHandler {

        @Override
        protected void readRequest(final DataInput input) throws IOException {
            int batchId = getContext().getHeader().getBatchId();
            ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_MESSAGE_SEVERITY);
            MessageSeverity severity = Enum.valueOf(MessageSeverity.class, input.readUTF());
            ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_MESSAGE);
            String message = input.readUTF();

            ExecuteRequestContext requestContext = activeRequests.get(batchId);
            if (requestContext == null) {
                throw new IOException("No active request found for " + batchId);
            }
            if (requestContext.getMessageHandler() != null) {
                requestContext.getMessageHandler().handleReport(severity, message);
            }
        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
        }
    }

    /**
     * Handles reads on the inputstreams returned by {@link OperationAttachments#getInputStreams()}
     * done in the remote target controller
     */
    private class ReadAttachmentInputStreamRequestHandler extends ManagementRequestHandler {
        InputStream attachmentInput;
        @Override
        protected void readRequest(final DataInput input) throws IOException {
            int batchId = getContext().getHeader().getBatchId();
            ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_INPUTSTREAM_INDEX);
            int index = input.readInt();

            ExecuteRequestContext requestContext = activeRequests.get(batchId);
            if (requestContext == null) {
                throw new IOException("No active request found for " + batchId);
            }
            InputStream in = requestContext.getAttachments().getInputStreams().get(index);
            attachmentInput = in != null ? new BufferedInputStream(in) : null;
        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            if (attachmentInput != null) {
                int i = attachmentInput.read();
                while (i != -1) {
                    bout.write(i);
                    i = attachmentInput.read();
                }
            }
            byte[] bytes = bout.toByteArray();
            output.write(NewModelControllerProtocol.PARAM_INPUTSTREAM_LENGTH);
            output.writeInt(bytes.length);
            output.write(NewModelControllerProtocol.PARAM_INPUTSTREAM_CONTENTS);
            output.write(bytes);
        }
    }

    private static class ExecuteRequestContext {
        final OperationMessageHandler messageHandler;
        final OperationAttachments attachments;

        public ExecuteRequestContext(final OperationMessageHandler messageHandler, final OperationAttachments attachments) {
            this.messageHandler = messageHandler;
            this.attachments = attachments;
        }

        public OperationMessageHandler getMessageHandler() {
            return messageHandler;
        }

        public OperationAttachments getAttachments() {
            return attachments;
        }
    }
}
