/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.remote;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.controller.NewModelController.OperationTransaction;
import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementBatchIdManager;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.old.ProtocolUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class NewRemoteProxyController implements NewProxyController, ManagementOperationHandler {

    private final AtomicBoolean closed = new AtomicBoolean();
    private final PathAddress pathAddress;
    private final ManagementChannel channel;
    private final ExecutorService executorService;
    private final Map<Integer, ExecuteRequestContext> activeRequests = Collections.synchronizedMap(new HashMap<Integer, ExecuteRequestContext>());
    private final ProxyOperationAddressTranslator addressTranslator;

    private NewRemoteProxyController(final ExecutorService executorService, final PathAddress pathAddress, final ProxyOperationAddressTranslator addressTranslator, final ManagementChannel channel) {
        this.pathAddress = pathAddress;
        this.channel = channel;
        this.executorService = executorService;
        this.addressTranslator = addressTranslator;

        channel.addCloseHandler(new CloseHandler<Channel>() {

            @Override
            public void handleClose(Channel closed) {
                NewRemoteProxyController.this.closed.set(true);
                synchronized (activeRequests) {
                    for (ExecuteRequestContext context : activeRequests.values()) {
                        context.setError("Channel closed");
                    }
                    activeRequests.clear();
                }
            }
        });
    }

    /**
     * Creates a new remote proxy controller using an exisiting channel
     *
     * @param executorService the executor to use for the requests
     * @param pathAddress the address within the model of the created proxy controller
     * @param addressTranslator the translator to use translating the address for the remote proxy
     * @param channel the channel to use for communication
     * @return the proxy controller
     */
    public static NewRemoteProxyController create(final ExecutorService executorService, final PathAddress pathAddress, final ProxyOperationAddressTranslator addressTranslator, final ManagementChannel channel) {
        return new NewRemoteProxyController(executorService, pathAddress, addressTranslator, channel);
    }

    /** {@inheritDoc} */
    @Override
    public PathAddress getProxyNodeAddress() {
        return pathAddress;
    }

    /** {@inheritDoc} */
    @Override
    public ManagementRequestHandler getRequestHandler(final byte id) {
        if (id == ModelControllerProtocol.HANDLE_REPORT_REQUEST) {
            return new HandleReportRequestHandler();
        } else if (id == ModelControllerProtocol.OPERATION_FAILED_REQUEST) {
            return new OperationFailedRequestHandler();
        } else if (id == ModelControllerProtocol.OPERATION_COMPLETED_REQUEST) {
            return new OperationCompletedRequestHandler();
        } else if (id == ModelControllerProtocol.OPERATION_PREPARED_REQUEST) {
            return new OperationPreparedRequestHandler();
        } else if (id == ModelControllerProtocol.GET_INPUTSTREAM_REQUEST) {
            return new ReadAttachmentInputStreamRequestHandler();
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ModelNode operation, final OperationMessageHandler handler, final ProxyOperationControl control, final OperationAttachments attachments) {
        final int batchId = ManagementBatchIdManager.DEFAULT.createBatchId();
        //As per the interface javadoc this method should block until either the operationFailed() or the operationPrepared() methods of the ProxyOperationControl have been called
        ExecuteRequest request = new ExecuteRequest(
            batchId,
            getOperationForProxy(operation),
            handler,
            new ProxyOperationControl() {
                @Override
                public void operationPrepared(OperationTransaction transaction, ModelNode result) {
                    control.operationPrepared(transaction, result);
                }

                @Override
                public void operationFailed(ModelNode response) {
                    control.operationFailed(response);
                }

                @Override
                public void operationCompleted(ModelNode response) {
                    control.operationCompleted(response);
                }
            },
            attachments);
        try {
            request.executeForResult(executorService, getChannelStrategy());
        } catch (Exception e) {
            try {
                ManagementBatchIdManager.DEFAULT.freeBatchId(batchId);
            } catch (Exception ignore) {
            }
            activeRequests.remove(batchId);
            throw new RuntimeException(e);
        }
    }

    private ManagementClientChannelStrategy getChannelStrategy() {
        return ManagementClientChannelStrategy.create(channel);
    }

    private ModelNode getOperationForProxy(final ModelNode op) {
        final PathAddress addr = PathAddress.pathAddress(op.get(OP_ADDR));
        final PathAddress translated = addressTranslator.translateAddress(addr);
        if (addr.equals(translated)) {
            return op;
        }
        final ModelNode proxyOp = op.clone();
        proxyOp.get(OP_ADDR).set(translated.toModelNode());
        return proxyOp;
    }

    /**
     * Propagates an execute() call from this proxy controller to the remote target controller
     */
    private class ExecuteRequest extends ManagementRequest<Void> {

        private final ExecuteRequestContext executeRequestContext;
        private final ModelNode operation;

        ExecuteRequest(final int batchId, final ModelNode operation, final OperationMessageHandler messageHandler, final ProxyOperationControl control, final OperationAttachments attachments) {
            super(batchId);
            this.operation = operation;
            executeRequestContext = new ExecuteRequestContext(this, messageHandler, control, attachments);
        }

        @Override
        protected byte getRequestCode() {
            return ModelControllerProtocol.EXECUTE_TX_REQUEST;
        }

        @Override
        protected void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
            boolean success = false;
            activeRequests.put(getBatchId(), executeRequestContext);

            try {
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
                success = true;
            } finally {
                if (!success) {
                    ManagementBatchIdManager.DEFAULT.freeBatchId(getBatchId());
                }
            }
        }

        @Override
        protected Void readResponse(final DataInput input) throws IOException {
            //TODO this needs cleaning up once the operation has been executed
            //activeRequests.remove(currentRequestId);
            return null;
        }

        protected void setError(Exception e) {
            super.setError(e);
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
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_MESSAGE_SEVERITY);
            MessageSeverity severity = Enum.valueOf(MessageSeverity.class, input.readUTF());
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_MESSAGE);
            String message = input.readUTF();

            ExecuteRequestContext requestContext = activeRequests.get(batchId);
            if (requestContext == null) {
                throw new IOException("No active request found for handling report " + batchId);
            }
            requestContext.getMessageHandler().handleReport(severity, message);
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
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAM_INDEX);
            int index = input.readInt();

            ExecuteRequestContext requestContext = activeRequests.get(batchId);
            if (requestContext == null) {
                throw new IOException("No active request found for reading inputstream report " + batchId);
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
            output.write(ModelControllerProtocol.PARAM_INPUTSTREAM_LENGTH);
            output.writeInt(bytes.length);
            output.write(ModelControllerProtocol.PARAM_INPUTSTREAM_CONTENTS);
            output.write(bytes);
        }
    }

    /**
     * Base class for handling {@link ProxyOperationControl} method calls done in the remote target controller
     */
    private abstract class ProxyOperationControlRequestHandler extends ManagementRequestHandler {
        volatile ExecuteRequestContext requestContext;
        @Override
        protected void readRequest(final DataInput input) throws IOException {
            int batchId = getContext().getHeader().getBatchId();
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_RESPONSE);
            ModelNode response = new ModelNode();
            response.readExternal(input);

            requestContext = activeRequests.get(batchId);
            if (requestContext == null) {
                throw new IOException("No active request found for proxy operation control " + batchId);
            }
            handle(batchId, requestContext.getControl(), response);
        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
        }

        abstract void handle(final int batchId, final ProxyOperationControl control, final ModelNode response) throws IOException;
    }

    /**
     * Handles {@link ProxyOperationControl#operationFailed(ModelNode)} method calls done in the remote target controller
     */
    private class OperationFailedRequestHandler extends ProxyOperationControlRequestHandler {

        @Override
        void handle(final int batchId, final ProxyOperationControl control, final ModelNode response) {
            activeRequests.remove(batchId);
            ManagementBatchIdManager.DEFAULT.freeBatchId(batchId);
            control.operationFailed(response);
        }
    }

    /**
     * Handles {@link ProxyOperationControl#operationFailed(ModelNode)} method calls done in the remote target controller
     */
    private class OperationCompletedRequestHandler extends ProxyOperationControlRequestHandler {

        @Override
        void handle(final int batchId, final ProxyOperationControl control, final ModelNode response) {
            ExecuteRequestContext context = activeRequests.remove(batchId);
            ManagementBatchIdManager.DEFAULT.freeBatchId(batchId);
            control.operationCompleted(response);
            context.setControlCompleted();
        }
    }

    /**
     * Handles {@link ProxyOperationControl#operationPrepared(org.jboss.as.controller.NewModelController.OperationTransaction, ModelNode)}
     * method calls done in the remote target controller
     */
    private class OperationPreparedRequestHandler extends ProxyOperationControlRequestHandler {
        volatile int status;

        @Override
        void handle(final int batchId, final ProxyOperationControl control, final ModelNode response)  throws IOException {
            control.operationPrepared(new OperationTransaction() {

                @Override
                public void rollback() {
                    done(false);
                }

                @Override
                public void commit() {
                    done(true);
                }

                private void done(boolean commit){
                    status = commit ? ModelControllerProtocol.PARAM_COMMIT : ModelControllerProtocol.PARAM_ROLLBACK;
                    requestContext.setTxCommittedOrRolledBack();
                    try {
                        requestContext.awaitControlCompleted();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("A timeout occurred waiting for the transaction to " + (commit ? "commit" : "rollback"));
                    }
                }
            }, response);
        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            output.write(ModelControllerProtocol.PARAM_PREPARED);
            output.flush();
            try {
                requestContext.awaitTxCommittedOrRolledBack();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Thread was interrupted waiting for Tx commit/rollback");
            }
            output.write(status);
        }
    }

    private static class ExecuteRequestContext {
        final ExecuteRequest request;
        final OperationMessageHandler messageHandler;
        final ProxyOperationControl control;
        final OperationAttachments attachments;
        final CountDownLatch controlCompletedLatch = new CountDownLatch(1);
        final CountDownLatch txCommittedOrRolledBackLatch = new CountDownLatch(1);
        volatile String error;

        public ExecuteRequestContext(final ExecuteRequest request, final OperationMessageHandler messageHandler, final ProxyOperationControl control, final OperationAttachments attachments) {
            this.request = request;
            this.messageHandler = messageHandler;
            this.control = control;
            this.attachments = attachments;
        }

        public OperationMessageHandler getMessageHandler() {
            return messageHandler;
        }

        public ProxyOperationControl getControl() {
            return control;
        }

        public OperationAttachments getAttachments() {
            return attachments;
        }

        void awaitControlCompleted() throws InterruptedException {
            controlCompletedLatch.await();
        }

        void setControlCompleted() {
            controlCompletedLatch.countDown();
        }

        void awaitTxCommittedOrRolledBack() throws InterruptedException {
            txCommittedOrRolledBackLatch.await();
            if (error != null) {
                throw new RuntimeException(error);
            }
        }

        void setTxCommittedOrRolledBack() {
            txCommittedOrRolledBackLatch.countDown();
            if (error != null) {
                throw new RuntimeException(error);
            }
        }

        synchronized void setError(String error) {
            this.error = error;
            txCommittedOrRolledBackLatch.countDown();
            controlCompletedLatch.countDown();
            request.setError(new Exception(error));
        }
    }

}
