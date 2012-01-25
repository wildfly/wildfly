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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;

import java.io.DataInput;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.protocol.ProtocolLogger;
import org.jboss.as.protocol.StreamUtils;
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

/**
 * The transactional request handler for a remote {@link ProxyController}.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Emanuel Muckenhuber
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
            executeRequestContext.operation = registerActiveOperation(request.getBatchId(), executeRequestContext);
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
        }
        return super.getRequestHandler(operationType);
    }

    /**
     * The request handler for requests from {@link RemoteProxyController#execute}.
     */
    private class ExecuteRequestHandler implements ManagementRequestHandler<Void, ExecuteRequestContext> {

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<ExecuteRequestContext> context) throws IOException {
            final ModelNode operation = new ModelNode();
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_OPERATION);
            operation.readExternal(input);
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAMS_LENGTH);
            final int attachmentsLength = input.readInt();
            context.executeAsync(new ManagementRequestContext.AsyncTask<ExecuteRequestContext>() {

                @Override
                public void execute(ManagementRequestContext<ExecuteRequestContext> context) throws Exception {
                    doExecute(operation, attachmentsLength, context);
                }

            });
        }

        protected void doExecute(final ModelNode operation, final int attachmentsLength, final ManagementRequestContext<ExecuteRequestContext> context) {
            final ExecuteRequestContext executeRequestContext = context.getAttachment();
            // Set the response information
            executeRequestContext.initialize(context);
            final Integer batchId = executeRequestContext.getOperationId();
            final AbstractModelControllerOperationHandler.OperationMessageHandlerProxy messageHandlerProxy = new AbstractModelControllerOperationHandler.OperationMessageHandlerProxy(context.getChannel(), batchId);
            final ProxyOperationControlProxy control = new ProxyOperationControlProxy(executeRequestContext);
            final AbstractModelControllerOperationHandler.OperationAttachmentsProxy attachmentsProxy = new AbstractModelControllerOperationHandler.OperationAttachmentsProxy(context.getChannel(), batchId, attachmentsLength);
            final ModelNode result;
            try {
                // Execute the operation
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
                // controller.execute() will block in OperationControl.prepared until the {@code PoxyController}
                // sent a CompleteTxRequest, which will either commit or rollback the operation
                control.operationCompleted(result);
            }
        }
    }

    /**
     * The request handler for requests from {@link RemoteProxyController.CompleteTxRequest}
     */
    private class CompleteTxOperationHandler implements ManagementRequestHandler<Void, ExecuteRequestContext> {

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<ExecuteRequestContext> context) throws IOException {
            final ExecuteRequestContext executeRequestContext = context.getAttachment();
            final byte commitOrRollback = input.readByte();

            // Complete transaction, either commit or rollback
            executeRequestContext.completeTx(context, commitOrRollback == ModelControllerProtocol.PARAM_COMMIT);
        }

    }

    static class ProxyOperationControlProxy implements ProxyController.ProxyOperationControl {

        private ExecuteRequestContext requestContext;
        ProxyOperationControlProxy(ExecuteRequestContext requestContext) {
            this.requestContext = requestContext;
        }

        @Override
        public void operationFailed(final ModelNode response) {
            requestContext.failed(response);
        }

        @Override
        public void operationCompleted(final ModelNode response) {
            requestContext.completed(response);
        }

        @Override
        public void operationPrepared(final ModelController.OperationTransaction transaction, final ModelNode result) {
            requestContext.prepare(transaction, result);
            try {
                // Wait for the commit or rollback message
                requestContext.txCompletedLatch.await();
            } catch (InterruptedException e) {
                requestContext.getResultHandler().failed(e);
                Thread.currentThread().interrupt();
            }
        }
    }

    static class ExecuteRequestContext {

        private boolean prepared;
        private ModelController.OperationTransaction activeTx;
        private ActiveOperation<Void, ExecuteRequestContext> operation;
        private ManagementRequestContext<ExecuteRequestContext> responseChannel;
        private final CountDownLatch txCompletedLatch = new CountDownLatch(1);

        Integer getOperationId() {
            return operation.getOperationId();
        }

        ActiveOperation.ResultHandler<Void> getResultHandler() {
            return operation.getResultHandler();
        }

        synchronized void initialize(final ManagementRequestContext<ExecuteRequestContext> context) {
            assert ! prepared;
            assert responseChannel == null;
            assert activeTx == null;
            // 1) initialize (set the response information)
            this.responseChannel = context;
        }

        synchronized void prepare(final ModelController.OperationTransaction tx, final ModelNode result) {
            assert !prepared;
            assert activeTx == null;
            assert responseChannel != null;
            try {
                // 2) send the operation-prepared notification (just clear response info)
                sendResponse(responseChannel, ModelControllerProtocol.PARAM_OPERATION_PREPARED, result);
                activeTx = tx;
                prepared = true;
            } catch (IOException e) {
                getResultHandler().failed(e);
            } finally {
                responseChannel = null;
            }
        }

        synchronized void completeTx(final ManagementRequestContext<ExecuteRequestContext> context, final boolean commit) {
            assert prepared;
            assert activeTx != null;
            assert responseChannel == null;
            responseChannel = context;
            if (commit) {
                activeTx.commit();
            } else {
                activeTx.rollback();
            }
            txCompletedLatch.countDown();
        }

        synchronized void failed(final ModelNode response) {
            if(prepared) {
                // in case commit or rollback throws an exception, to conform with the API we still send a operation-completed message
                completed(response);
            } else {
                assert responseChannel != null;
                try {
                    // 2) send the operation-failed message (done)
                    sendResponse(responseChannel, ModelControllerProtocol.PARAM_OPERATION_FAILED, response);
                } catch (IOException e) {
                    ProtocolLogger.ROOT_LOGGER.debugf(e, "failed to process message");
                } finally {
                    getResultHandler().done(null);
                }
            }
        }

        synchronized void completed(final ModelNode response) {
            assert prepared;
            assert responseChannel != null;
            try {
                // 4) operation-completed (done)
                sendResponse(responseChannel, ModelControllerProtocol.PARAM_OPERATION_COMPLETED, response);
            } catch (IOException e) {
                ProtocolLogger.ROOT_LOGGER.debugf(e, "failed to process message");
            } finally {
                getResultHandler().done(null);
            }
        }

    }

    /**
     * Send a operation response.
     *
     * @param context the request context
     * @param responseType the response type
     * @param response the operation response
     * @throws IOException for any error
     */
    static void sendResponse(final ManagementRequestContext<ExecuteRequestContext> context, final byte responseType, final ModelNode response) throws IOException {
        final ManagementResponseHeader header = ManagementResponseHeader.create(context.getRequestHeader());
        final FlushableDataOutput output = context.writeMessage(header);
        try {
            // response type
            output.writeByte(responseType);
            // operation result
            response.writeExternal(output);
            // response end
            output.writeByte(ManagementProtocol.RESPONSE_END);
            output.close();
        } finally {
            StreamUtils.safeClose(output);
        }

    }

}
