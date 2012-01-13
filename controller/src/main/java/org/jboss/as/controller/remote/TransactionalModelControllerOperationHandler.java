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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;

import java.io.DataInput;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementProtocolHeader;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ManagementResponseHeader;
import org.jboss.as.protocol.mgmt.ProtocolUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;

/**
 * This model controller relies on the clients connecting with
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TransactionalModelControllerOperationHandler extends AbstractModelControllerOperationHandler<Void, TransactionalModelControllerOperationHandler.ExecuteRequestContext> {

    private final ModelController controller;
    public TransactionalModelControllerOperationHandler(final ModelController controller, final ExecutorService executorService) {
        super(executorService);
        this.controller = controller;
    }

    @Override
    protected ManagementRequestHeader validateRequest(ManagementProtocolHeader header) throws IOException {
        final ManagementRequestHeader request =  super.validateRequest(header);
        // Initialize the request context
        if(request.getOperationId() == ModelControllerProtocol.EXECUTE_TX_REQUEST) {
            final ExecuteRequestContext executeRequestContext = new ExecuteRequestContext();
            final ActiveOperation<Void, ExecuteRequestContext> support = registerActiveOperation(request.getBatchId(), executeRequestContext);
            executeRequestContext.setActiveOperation(support);

        } else if (request.getOperationId() == ModelControllerProtocol.LEGACY_MASTER_HC_PING_REQUEST) {
            registerActiveOperation(request.getBatchId(), new ExecuteRequestContext());
        }
        return request;
    }

    @Override
    protected ManagementRequestHandler<Void, ExecuteRequestContext> getRequestHandler(byte operationType) {
        switch(operationType) {
            case ModelControllerProtocol.EXECUTE_TX_REQUEST:
                return new ExecuteRequestHandler();
            case ModelControllerProtocol.COMPLETE_TX_REQUEST:
                return new CompleteTxOperationHandler();
            case ModelControllerProtocol.LEGACY_MASTER_HC_PING_REQUEST:
                return new LegacyMasterHcPingRequestHandler();
        }
        return super.getRequestHandler(operationType);
    }

    private static class LegacyMasterHcPingRequestHandler implements ManagementRequestHandler<Void, TransactionalModelControllerOperationHandler.ExecuteRequestContext> {
        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<ExecuteRequestContext> context) throws IOException {
            context.executeAsync(ProtocolUtils.<ExecuteRequestContext>emptyResponseTask());
            resultHandler.done(null);
        }
    }

    /**
     * Execute a request.
     */
    private class ExecuteRequestHandler implements ManagementRequestHandler<Void, ExecuteRequestContext> {

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<ExecuteRequestContext> context) throws IOException {
            final ModelNode operation = new ModelNode();
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_OPERATION);
            operation.readExternal(input);
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAMS_LENGTH);
            final int attachmentsLength = input.readInt();
            // For backwards compatibility we have to send an empty response?
            final ManagementResponseHeader response = ManagementResponseHeader.create(context.getRequestHeader());
            final FlushableDataOutput os = context.writeMessage(response);
            try {
                os.write(ManagementProtocol.RESPONSE_END);
                os.close();
            } finally {
                StreamUtils.safeClose(os);
            }
            // Execute the actual task
            context.executeAsync(new ManagementRequestContext.AsyncTask<ExecuteRequestContext>() {
                @Override
                public void execute(ManagementRequestContext<ExecuteRequestContext> context) throws Exception {
                    doExecute(operation, attachmentsLength, context);
                }
            });

        }

        protected void doExecute(final ModelNode operation, final int attachmentsLength, final ManagementRequestContext<ExecuteRequestContext> context) {
            final ExecuteRequestContext executeRequestContext = context.getAttachment();
            final Integer batchId = executeRequestContext.getBatchId();
            final AbstractModelControllerOperationHandler.OperationMessageHandlerProxy messageHandlerProxy = new AbstractModelControllerOperationHandler.OperationMessageHandlerProxy(context.getChannel(), batchId);
            final ProxyOperationControlProxy control = new ProxyOperationControlProxy(context.getChannel(), batchId, executeRequestContext);
            final AbstractModelControllerOperationHandler.OperationAttachmentsProxy attachmentsProxy = new AbstractModelControllerOperationHandler.OperationAttachmentsProxy(context.getChannel(), batchId, attachmentsLength);
            final ModelNode result;
            try {
                result = controller.execute(
                        operation,
                        messageHandlerProxy,
                        control,
                        attachmentsProxy);
            } catch (Exception e) {
                final ModelNode failure = new ModelNode();
                failure.get(OUTCOME).set(FAILED);
                failure.get(FAILURE_DESCRIPTION).set(e.getClass().getName() + ":" + e.getMessage());
                control.operationFailed(failure);
                attachmentsProxy.shutdown(e);
                return;
            }
            if (result.hasDefined(FAILURE_DESCRIPTION)) {
                control.operationFailed(result);
            } else {
                // The model controller will block in OperationControl.prepared until the {@code ProxyStepHandler}
                // send a message to either commit or rollback the current operation
                control.operationCompleted(result);
            }
        }

    }

    /**
     * Handler which tells the OperationControl to either commit or rollback.
     */
    private class CompleteTxOperationHandler implements ManagementRequestHandler<Void, TransactionalModelControllerOperationHandler.ExecuteRequestContext> {

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<ExecuteRequestContext> context) throws IOException {
            final byte commitOrRollback = input.readByte();
            final ExecuteRequestContext executeRequestContext = context.getAttachment();
            executeRequestContext.completeTx(commitOrRollback == ModelControllerProtocol.PARAM_COMMIT);
            context.executeAsync(ProtocolUtils.<ExecuteRequestContext>emptyResponseTask());
        }

    }

    /**
     * A proxy to the proxy operation control proxy on the remote caller.
     */
    private class ProxyOperationControlProxy implements ProxyController.ProxyOperationControl {
        private final int batchId;
        private final Channel channel;
        private final ExecuteRequestContext executeRequestContext;

        public ProxyOperationControlProxy(final Channel channel, final int batchId, final ExecuteRequestContext executeRequestContext) {
            this.batchId = batchId;
            this.channel = channel;
            this.executeRequestContext = executeRequestContext;
        }

        @Override
        public void operationPrepared(final ModelController.OperationTransaction transaction, final ModelNode result) {
            try {
                executeRequest(new OperationStatusRequest(result) {
                    @Override
                    public byte getOperationType() {
                        return ModelControllerProtocol.OPERATION_PREPARED_REQUEST;
                    }

                    @Override
                    protected void sendRequest(ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<ExecuteRequestContext> context, FlushableDataOutput output) throws IOException {
                        executeRequestContext.setActiveTX(transaction);
                        super.sendRequest(resultHandler, context, output);
                    }

                    @Override
                    public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<ExecuteRequestContext> context) throws IOException {
                        //
                    }
                });
            } catch (Exception e) {
                executeRequestContext.handleFailed(e);
                throw new RuntimeException(e);
            }

            try {
                // Block until we receive the {@code ModelControllerProtocol.COMPLETE_TX_REQUEST}
                executeRequestContext.awaitTxCompleted();
            } catch (InterruptedException e) {
                executeRequestContext.handleFailed(e);
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void operationFailed(final ModelNode response) {
            executeRequest(new OperationStatusRequest(response) {

                @Override
                public byte getOperationType() {
                    return ModelControllerProtocol.OPERATION_FAILED_REQUEST;
                }

                @Override
                public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<ExecuteRequestContext> context) throws IOException {
                    // Mark the active operation as complete. Even though the operation failed we don't want to throw an exception.
                    executeRequestContext.getResultHandler().done(null);
                }

            });
        }

        @Override
        public void operationCompleted(final ModelNode response) {
            executeRequest(new OperationStatusRequest(response) {

                @Override
                public byte getOperationType() {
                    return ModelControllerProtocol.OPERATION_COMPLETED_REQUEST;
                }

                @Override
                public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<ExecuteRequestContext> context) throws IOException {
                    // Mark the active operation as complete.
                    executeRequestContext.getResultHandler().done(null);
                }
            });

        }

        protected void executeRequest(ManagementRequest<Void, ExecuteRequestContext> request) {
            try {
                executeRequest(channel, batchId, request);
            } catch(Exception e) {
                executeRequestContext.handleFailed(e);
            }
        }

        protected Future<Void> executeRequest(final Channel channel, final int batchId, ManagementRequest<Void, ExecuteRequestContext> request) {
            final ActiveOperation<Void, ExecuteRequestContext> support = TransactionalModelControllerOperationHandler.this.getActiveOperation(batchId);
            if(support == null) {
                throw MESSAGES.noActiveTransaction(batchId);
            }
            return TransactionalModelControllerOperationHandler.this.executeRequest(request, channel, support);
        }
    }

    abstract class OperationStatusRequest extends AbstractManagementRequest<Void, ExecuteRequestContext> {

        private final ModelNode response;

        protected OperationStatusRequest(ModelNode response) {
            this.response = response;
        }

        @Override
        protected void sendRequest(final ActiveOperation.ResultHandler<Void> resultHandler,
                                   final ManagementRequestContext<ExecuteRequestContext> context,
                                   final FlushableDataOutput output) throws IOException {
            output.write(ModelControllerProtocol.PARAM_RESPONSE);
            response.writeExternal(output);

        }

    }

    static class ExecuteRequestContext {
        private ModelController.OperationTransaction activeTx;
        private ActiveOperation<Void, ExecuteRequestContext> operation;
        private final CountDownLatch txCompletedLatch = new CountDownLatch(1);

        synchronized void setActiveOperation(ActiveOperation<Void, ExecuteRequestContext> operation) {
            assert operation != null;
            this.operation = operation;
        }

        synchronized void setActiveTX(ModelController.OperationTransaction activeTx) {
            this.activeTx = activeTx;
        }

        synchronized void completeTx(boolean commit) {
            if (commit) {
                activeTx.commit();
            } else {
                activeTx.rollback();
            }
            //
            txCompletedLatch.countDown();
        }

        synchronized ActiveOperation.ResultHandler<Void> getResultHandler() {
            return operation.getResultHandler();
        }

        synchronized Integer getBatchId() {
            return operation.getOperationId();
        }

        void handleFailed(final Exception e) {
            getResultHandler().failed(e);
            txCompletedLatch.countDown();
        }

        void awaitTxCompleted() throws InterruptedException {
            txCompletedLatch.await();
        }

    }
}