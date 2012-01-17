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
package org.jboss.as.controller.remote;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.protocol.mgmt.ProtocolUtils.expectHeader;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.controller.ModelController.OperationTransaction;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementProtocolHeader;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ManagementResponseHeader;
import org.jboss.as.protocol.mgmt.ProtocolUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RemoteProxyController extends AbstractMessageHandler<Void, RemoteProxyController.ExecuteRequestContext> implements ProxyController {

    private final PathAddress pathAddress;
    private final Channel channel;
    private final ProxyOperationAddressTranslator addressTranslator;

    private RemoteProxyController(final ExecutorService executorService, final PathAddress pathAddress,
                                  final ProxyOperationAddressTranslator addressTranslator, final Channel channel) {
        super(executorService);
        this.pathAddress = pathAddress;
        this.channel = channel;
        this.addressTranslator = addressTranslator;
    }

    /**
     * Creates a new remote proxy controller using an existing channel
     *
     * @param executorService the executor to use for the requests
     * @param pathAddress the address within the model of the created proxy controller
     * @param addressTranslator the translator to use translating the address for the remote proxy
     * @param channel the channel to use for communication
     * @return the proxy controller
     */
    public static RemoteProxyController create(final ExecutorService executorService, final PathAddress pathAddress, final ProxyOperationAddressTranslator addressTranslator, final Channel channel) {
        return new RemoteProxyController(executorService, pathAddress, addressTranslator, channel);
    }

    /** {@inheritDoc} */
    @Override
    public PathAddress getProxyNodeAddress() {
        return pathAddress;
    }

    @Override
    protected ManagementRequestHeader validateRequest(ManagementProtocolHeader header) throws IOException {
        ManagementRequestHeader request = super.validateRequest(header);
        return request;
    }

    @Override
    protected ManagementRequestHandler<Void, ExecuteRequestContext> getRequestHandler(byte operationType) {
        if (operationType == ModelControllerProtocol.HANDLE_REPORT_REQUEST) {
            return new HandleReportRequestHandler();
        } else if (operationType == ModelControllerProtocol.OPERATION_FAILED_REQUEST) {
            return new OperationFailedRequestHandler();
        } else if (operationType == ModelControllerProtocol.OPERATION_COMPLETED_REQUEST) {
            return new OperationCompletedRequestHandler();
        } else if (operationType == ModelControllerProtocol.OPERATION_PREPARED_REQUEST) {
            return new OperationPreparedRequestHandler();
        } else if (operationType == ModelControllerProtocol.GET_INPUTSTREAM_REQUEST) {
            return new ReadAttachmentInputStreamRequestHandler();
        }
        return super.getRequestHandler(operationType);
    }


    /** {@inheritDoc} */
    @Override
    public void execute(final ModelNode original, final OperationMessageHandler handler, final ProxyOperationControl control, final OperationAttachments attachments) {
        // Translate the operation address first
        final ModelNode operation = getOperationForProxy(original);
        final ExecuteRequestContext context = new ExecuteRequestContext(operation, attachments, handler, control);
        try {
            final ActiveOperation<Void, RemoteProxyController.ExecuteRequestContext> support = super.registerActiveOperation(context, context);
            super.executeRequest(new ExecuteRequest(), channel, support);
            // Wait until the remote side completed all steps and sends the prepared
            // result, or there is a failure
            context.awaitPreparedOrFailed();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Translate the operation address.
     *
     * @param op the operation
     * @return the new operation
     */
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

    private class ExecuteRequest extends AbstractManagementRequest<Void, ExecuteRequestContext> {

        @Override
        public byte getOperationType() {
            return ModelControllerProtocol.EXECUTE_TX_REQUEST;
        }

        @Override
        protected void sendRequest(final ActiveOperation.ResultHandler<Void> resultHandler,
                                   final ManagementRequestContext<ExecuteRequestContext> context,
                                   final FlushableDataOutput output) throws IOException {
            // Write the operation
            final ExecuteRequestContext executionContext = context.getAttachment();
            final List<InputStream> streams = executionContext.getInputStreams();
            final ModelNode operation = executionContext.getOperation();
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
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<ExecuteRequestContext> context) throws IOException {
            //
        }
    }

    /**
     * Handles {@link OperationMessageHandler#handleReport(org.jboss.as.controller.client.MessageSeverity, String)} calls
     * done in the remote target controller
     */
    private class HandleReportRequestHandler implements ManagementRequestHandler<Void, ExecuteRequestContext> {

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<ExecuteRequestContext> context) throws IOException {
            expectHeader(input, ModelControllerProtocol.PARAM_MESSAGE_SEVERITY);
            final MessageSeverity severity = Enum.valueOf(MessageSeverity.class, input.readUTF());
            expectHeader(input, ModelControllerProtocol.PARAM_MESSAGE);
            final String message = input.readUTF();
            expectHeader(input, ManagementProtocol.REQUEST_END);

            final ExecuteRequestContext requestContext = context.getAttachment();
            // perhaps execute async
            final OperationMessageHandler handler = requestContext.getMessageHandler();
            handler.handleReport(severity, message);
        }

    }

    /**
     * Handles reads on the inputstreams returned by {@link OperationAttachments#getInputStreams()}
     * done in the remote target controller
     */
    private class ReadAttachmentInputStreamRequestHandler implements ManagementRequestHandler<Void, ExecuteRequestContext> {

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler,
                              final ManagementRequestContext<ExecuteRequestContext> context) throws IOException {
            // Read the inputStream index
            expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAM_INDEX);
            final int index = input.readInt();

            context.executeAsync(new ManagementRequestContext.AsyncTask<ExecuteRequestContext>() {
                @Override
                public void execute(final ManagementRequestContext<ExecuteRequestContext> context) throws Exception {
                    final ExecuteRequestContext exec = context.getAttachment();
                    final ManagementRequestHeader header = ManagementRequestHeader.class.cast(context.getRequestHeader());
                    final ManagementResponseHeader response = new ManagementResponseHeader(header.getVersion(), header.getRequestId(), null);
                    final InputStream is = exec.getAttachments().getInputStreams().get(index);
                    try {
                        final ByteArrayOutputStream bout = copyStream(is);
                        final FlushableDataOutput output = context.writeMessage(response);
                        try {
                            output.writeByte(ModelControllerProtocol.PARAM_INPUTSTREAM_LENGTH);
                            output.writeInt(bout.size());
                            output.writeByte(ModelControllerProtocol.PARAM_INPUTSTREAM_CONTENTS);
                            output.write(bout.toByteArray());
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
            });
        }

        protected ByteArrayOutputStream copyStream(final InputStream is) throws IOException {
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();
            if(is != null) {
                StreamUtils.copyStream(is, bout);
            }
            return bout;
        }

    }

    /**
     * Base class for handling {@link ProxyOperationControl} method calls done in the remote target controller
     */
    abstract class ProxyOperationControlRequestHandler implements ManagementRequestHandler<Void, ExecuteRequestContext> {

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler,
                                  final ManagementRequestContext<ExecuteRequestContext> context) throws IOException {
            expectHeader(input, ModelControllerProtocol.PARAM_RESPONSE);
            final ModelNode response = new ModelNode();
            response.readExternal(input);
            // handle
            handle(response, resultHandler, context);
            // TODO we actually don't need all this empty messages
            context.executeAsync(ProtocolUtils.<ExecuteRequestContext>emptyResponseTask());
        }

        abstract void handle(ModelNode response, ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<ExecuteRequestContext> context);

    }

    /**
     * Handles {@link ProxyOperationControl#operationFailed(ModelNode)} method calls done in the remote target controller
     */
    private class OperationFailedRequestHandler extends ProxyOperationControlRequestHandler {

        @Override
        void handle(final ModelNode response, final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<ExecuteRequestContext> context) {
            final ExecuteRequestContext executeRequestContext = context.getAttachment();
            executeRequestContext.getControl().operationFailed(response);
            // complete ?
        }

    }

    /**
     * Handles {@link ProxyOperationControl#operationFailed(ModelNode)} method calls done in the remote target controller
     */
    private class OperationCompletedRequestHandler extends ProxyOperationControlRequestHandler {

        void handle(final ModelNode response, final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<ExecuteRequestContext> context) {
            final ExecuteRequestContext executeRequestContext = context.getAttachment();
            executeRequestContext.getControl().operationCompleted(response);
            resultHandler.done(null);
        }

    }

    /**
     * Handles {@link ProxyOperationControl#operationPrepared(org.jboss.as.controller.ModelController.OperationTransaction, ModelNode)}
     * method calls done in the remote target controller
     */
    private class OperationPreparedRequestHandler extends ProxyOperationControlRequestHandler {

        @Override
        void handle(final ModelNode response, final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<ExecuteRequestContext> context) {
            final ExecuteRequestContext executeRequestContext = context.getAttachment();
            executeRequestContext.getControl().operationPrepared(new OperationTransaction() {

                @Override
                public void rollback() {
                    done(false);
                }

                @Override
                public void commit() {
                    done(true);
                }

                private void done(boolean commit){
                    final byte status = commit ? ModelControllerProtocol.PARAM_COMMIT : ModelControllerProtocol.PARAM_ROLLBACK;
                    final ActiveOperation<Void, ExecuteRequestContext> activeOperation = RemoteProxyController.this.getActiveOperation(context.getOperationId());
                    try {
                        RemoteProxyController.this.executeRequest(new AbstractManagementRequest<Void, ExecuteRequestContext>() {

                            @Override
                            public byte getOperationType() {
                                return ModelControllerProtocol. COMPLETE_TX_REQUEST;
                            }

                            @Override
                            protected void sendRequest(ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<ExecuteRequestContext> executeRequestContextManagementRequestContext, FlushableDataOutput output) throws IOException {
                                output.write(status);
                            }

                            @Override
                            public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<ExecuteRequestContext> executeRequestContextManagementRequestContext) throws IOException {
                                //
                            }
                        }, channel, activeOperation);
                    } catch (Exception e) {
                        resultHandler.failed(e);
                    }
                    try {
                        /// Await the operation completed notification
                        activeOperation.getResult().await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw MESSAGES.transactionTimeout(commit ? "commit" : "rollback");
                    }
                }
            }, response);
        }
    }

    static class ExecuteRequestContext implements ActiveOperation.CompletedCallback<Void> {

        final ModelNode operation;
        final OperationAttachments attachments;
        final OperationMessageHandler messageHandler;
        final ProxyOperationControl control;
        final AtomicBoolean completed = new AtomicBoolean(false);
        final CountDownLatch prepareOrFailedLatch = new CountDownLatch(1);

        public ExecuteRequestContext(final ModelNode operation, final OperationAttachments attachments, final OperationMessageHandler messageHandler, final ProxyOperationControl delegate) {
            this.control = new ProxyOperationControl() {
                @Override
                public void operationFailed(ModelNode response) {
                    if(completed.compareAndSet(false, true)) {
                        delegate.operationFailed(response);
                    }
                    prepareOrFailedLatch.countDown();
                }

                @Override
                public void operationCompleted(ModelNode response) {
                    if(completed.compareAndSet(false, true)) {
                        delegate.operationCompleted(response);
                    }
                }

                @Override
                public void operationPrepared(OperationTransaction transaction, ModelNode result) {
                    delegate.operationPrepared(transaction, result);
                    prepareOrFailedLatch.countDown();
                }
            };
            this.operation = operation;
            this.attachments = attachments;
            this.messageHandler = messageHandler;
        }

        public OperationMessageHandler getMessageHandler() {
            return messageHandler;
        }

        public ModelNode getOperation() {
            return operation;
        }

        public OperationAttachments getAttachments() {
            return attachments;
        }

        public List<InputStream> getInputStreams() {
            final OperationAttachments attachments = getAttachments();
            if(attachments == null) {
                return Collections.emptyList();
            }
            return attachments.getInputStreams();
        }

        public ProxyOperationControl getControl() {
            return control;
        }

        public void awaitPreparedOrFailed() throws InterruptedException {
            prepareOrFailedLatch.await();
        }

        @Override
        public void completed(Void result) {
            //
        }

        @Override
        public void failed(final Exception e) {
            control.operationFailed(getResponse("failed"));
            prepareOrFailedLatch.countDown();
        }

        @Override
        public void cancelled() {
            control.operationFailed(getResponse("cancelled"));
            prepareOrFailedLatch.countDown();
        }

    }

    static ModelNode getResponse(final String outcome) {
        final ModelNode response = new ModelNode();
        response.get(OUTCOME).set(outcome);
        return response;
    }

}
