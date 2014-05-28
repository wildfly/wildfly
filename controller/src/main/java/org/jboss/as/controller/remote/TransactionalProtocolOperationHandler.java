/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.logging.ControllerLogger.MGMT_OP_LOGGER;
import static org.jboss.as.controller.logging.ControllerLogger.ROOT_LOGGER;

import java.io.DataInput;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.CountDownLatch;

import javax.security.auth.Subject;

import org.jboss.as.controller.AccessAuditContext;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelAssociation;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.protocol.mgmt.ManagementRequestContext.AsyncTask;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestHandlerFactory;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ManagementResponseHeader;
import org.jboss.as.protocol.mgmt.ProtocolUtils;
import org.jboss.dmr.ModelNode;

/**
 * The transactional request handler for a remote {@link TransactionalProtocolClient}.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class TransactionalProtocolOperationHandler implements ManagementRequestHandlerFactory {

    private final ModelController controller;
    private final ManagementChannelAssociation channelAssociation;
    public TransactionalProtocolOperationHandler(final ModelController controller, final ManagementChannelAssociation channelAssociation) {
        this.controller = controller;
        this.channelAssociation = channelAssociation;
    }

    @Override
    public ManagementRequestHandler<?, ?> resolveHandler(RequestHandlerChain handlers, ManagementRequestHeader request) {
        switch(request.getOperationId()) {
            case ModelControllerProtocol.EXECUTE_TX_REQUEST: {
                // Initialize the request context
                final ExecuteRequestContext executeRequestContext = new ExecuteRequestContext();
                try {
                    executeRequestContext.operation = handlers.registerActiveOperation(request.getBatchId(), executeRequestContext, executeRequestContext);
                } catch (IllegalStateException ise) {
                    // WFLY-3381 Unusual case where the initial request lost a race with a COMPLETE_TX_REQUEST carrying a cancellation
                    return new AbortOperationHandler(true);
                }
                return new ExecuteRequestHandler();
            }
            case ModelControllerProtocol.COMPLETE_TX_REQUEST: {
                final ExecuteRequestContext executeRequestContext = new ExecuteRequestContext();
                try {
                    executeRequestContext.operation = handlers.registerActiveOperation(request.getBatchId(), executeRequestContext, executeRequestContext);
                    // WLFY-3381 Unusual case where the initial request must have lost a race with a COMPLETE_TX_REQUEST carrying a cancellation
                    return new AbortOperationHandler(false);
                } catch (IllegalStateException ise) {
                    // Expected case -- not a normal commit/rollback or one where a COMPLETE_TX_REQUEST with a cancel
                    // won a race with the initial request
                }
                return new CompleteTxOperationHandler();
            }
        }
        return handlers.resolveNext();
    }

    /**
     * The request handler for requests from {@link org.jboss.as.controller.remote.TransactionalProtocolClient#execute}.
     */
    private class ExecuteRequestHandler implements ManagementRequestHandler<Void, ExecuteRequestContext> {

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<ExecuteRequestContext> context) throws IOException {
            ControllerLogger.MGMT_OP_LOGGER.tracef("Handling transactional ExecuteRequest for %d", context.getOperationId());

            final ExecutableRequest executableRequest = ExecutableRequest.parse(input, channelAssociation);

            final PrivilegedAction<Void> action = new PrivilegedAction<Void>() {

                @Override
                public Void run() {
                    doExecute(executableRequest.operation, executableRequest.attachmentsLength, context);
                    return null;
                }
            };

            // Set the response information and execute the operation
            final ExecuteRequestContext executeRequestContext = context.getAttachment();
            executeRequestContext.initialize(context);
            context.executeAsync(new AsyncTask<TransactionalProtocolOperationHandler.ExecuteRequestContext>() {

                @Override
                public void execute(final ManagementRequestContext<ExecuteRequestContext> context) throws Exception {
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {

                        @Override
                        public Void run() {
                            AccessAuditContext.doAs(executableRequest.subject, action);
                            return null;
                        }
                    });

                }
            });
        }

        protected void doExecute(final ModelNode operation, final int attachmentsLength, final ManagementRequestContext<ExecuteRequestContext> context) {
            ControllerLogger.MGMT_OP_LOGGER.tracef("Executing transactional ExecuteRequest for %d", context.getOperationId());
            final ExecuteRequestContext executeRequestContext = context.getAttachment();
            // Set the response information
            executeRequestContext.initialize(context);
            final Integer batchId = executeRequestContext.getOperationId();
            final OperationMessageHandlerProxy messageHandlerProxy = new OperationMessageHandlerProxy(channelAssociation, batchId);
            final ProxyOperationControlProxy control = new ProxyOperationControlProxy(executeRequestContext);
            final OperationAttachmentsProxy attachmentsProxy = OperationAttachmentsProxy.create(channelAssociation, batchId, attachmentsLength);
            final ModelNode result;
            try {
                // Execute the operation
                result = internalExecute(operation, context, messageHandlerProxy, control, attachmentsProxy);
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
                // controller.execute() will block in OperationControl.prepared until the {@code ProxyController}
                // sent a CompleteTxRequest, which will either commit or rollback the operation
                control.operationCompleted(result);
            }
        }
    }

    private static class ExecutableRequest {
        private final ModelNode operation;
        private final int attachmentsLength;
        private final Subject subject;

        private ExecutableRequest(ModelNode operation, int attachmentsLength, Subject subject) {
            this.operation = operation;
            this.attachmentsLength = attachmentsLength;
            this.subject = subject;
        }

        static ExecutableRequest parse(DataInput input, ManagementChannelAssociation channelAssociation) throws IOException {
            final ModelNode operation = new ModelNode();
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_OPERATION);
            operation.readExternal(input);
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAMS_LENGTH);
            final int attachmentsLength = input.readInt();

            final Subject subject;
            final Boolean readSubject = channelAssociation.getAttachments().getAttachment(TransactionalProtocolClient.SEND_SUBJECT);
            if (readSubject != null && readSubject) {
                subject = readSubject(input);
            } else {
                subject = new Subject();
            }
            return new ExecutableRequest(operation, attachmentsLength, subject);
        }
    }

    /**
     * Subclasses can override this method to determine how to execute the method, e.g. attach to an existing operation or not
     *
     * @param operation the operation being executed
     * @param messageHandler the operation message handler proxy
     * @param control the operation transaction control
     * @param attachments the operation attachments proxy
     * @return the result of the executed operation
     */
    protected ModelNode internalExecute(final ModelNode operation, final ManagementRequestContext<?> context, final OperationMessageHandler messageHandler, final ProxyController.ProxyOperationControl control, OperationAttachments attachments) {
        // Execute the operation
        return controller.execute(
                operation,
                messageHandler,
                control,
                attachments);
    }

    /**
     * The request handler for requests from {@link org.jboss.as.controller.remote.TransactionalProtocolClientImpl.CompleteTxRequest}
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

    private class AbortOperationHandler implements ManagementRequestHandler<Void, ExecuteRequestContext> {

        private final boolean forExecuteTxRequest;

        private AbortOperationHandler(boolean forExecuteTxRequest) {
            this.forExecuteTxRequest = forExecuteTxRequest;
        }

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<ExecuteRequestContext> context) throws IOException {

            if (forExecuteTxRequest) {
                try {
                    // Read and discard the input
                    ExecutableRequest.parse(input, channelAssociation);
                } finally {
                    ControllerLogger.MGMT_OP_LOGGER.tracef("aborting (cancel received before request) for %d", context.getOperationId());
                    ModelNode response = new ModelNode();
                    response.get(OUTCOME).set(CANCELLED);
                    context.getAttachment().failed(response);
                }
            } else {
                // This was a COMPLETE_TX_REQUEST that came in before the original EXECUTE_TX_REQUEST
                final byte commitOrRollback = input.readByte();
                if (commitOrRollback == ModelControllerProtocol.PARAM_COMMIT) {
                    // a cancel would not use PARAM_COMMIT; this was a request that didn't match any existing op
                    // Likely the request was cancelled and removed but the commit message was in process
                    throw ControllerLogger.MGMT_OP_LOGGER.responseHandlerNotFound(context.getOperationId());
                }
                // else this was a cancel request. Do nothing and wait for the initial operation request to come in
                // and see the pre-existing ActiveOperation and then call this with forExecuteTxRequest=true
            }
        }

    }

    static class ProxyOperationControlProxy implements ProxyController.ProxyOperationControl {

        private final ExecuteRequestContext requestContext;
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
                // requestContext.getResultHandler().failed(e);
                ROOT_LOGGER.tracef("Clearing interrupted status from client request %d", requestContext.getOperationId());
                Thread.currentThread().interrupt();
            }
        }
    }

    static class ExecuteRequestContext implements ActiveOperation.CompletedCallback<Void> {

        private boolean prepared;
        private boolean rollbackOnPrepare;
        private ModelController.OperationTransaction activeTx;
        private ActiveOperation<Void, ExecuteRequestContext> operation;
        private ManagementRequestContext<ExecuteRequestContext> responseChannel;
        private final CountDownLatch txCompletedLatch = new CountDownLatch(1);
        private boolean txCompleted;
        private PrivilegedAction<Void> action;

        Integer getOperationId() {
            return operation.getOperationId();
        }

        ActiveOperation.ResultHandler<Void> getResultHandler() {
            return operation.getResultHandler();
        }

        public PrivilegedAction<Void> getAction() {
            return action;
        }

        public void setAction(PrivilegedAction<Void> action) {
            this.action = action;
        }

        @Override
        public void completed(Void result) {
            //
        }

        @Override
        public synchronized void failed(Exception e) {
            if(prepared) {
                final ModelController.OperationTransaction transaction = activeTx;
                if(transaction != null) {
                    transaction.rollback();
                    txCompletedLatch.countDown();
                }
            } else if (responseChannel != null) {
                rollbackOnPrepare = true;
                // Failed in a step before prepare, send error response
                final String message = e.getMessage() != null ? e.getMessage() : "failure before rollback " + e.getClass().getName();
                final ModelNode response = new ModelNode();
                response.get(OUTCOME).set(FAILED);
                response.get(FAILURE_DESCRIPTION).set(message);
                ControllerLogger.MGMT_OP_LOGGER.tracef("sending pre-prepare failed response for %d  --- interrupted: %s", getOperationId(), Thread.currentThread().isInterrupted());
                try {
                    sendResponse(responseChannel, ModelControllerProtocol.PARAM_OPERATION_FAILED, response);
                } catch (IOException ignored) {
                    ControllerLogger.MGMT_OP_LOGGER.debugf(ignored, "failed to process message");
                }
            }
        }

        @Override
        public void cancelled() {
            //
        }

        synchronized void initialize(final ManagementRequestContext<ExecuteRequestContext> context) {
            assert ! prepared;
            assert activeTx == null;
            // 1) initialize (set the response information)
            this.responseChannel = context;
        }

        synchronized void prepare(final ModelController.OperationTransaction tx, final ModelNode result) {
            if(rollbackOnPrepare) {
                try {
                    tx.rollback();
                } finally {
                    txCompletedLatch.countDown();
                }

                // TODO send response ?

            } else {
                assert !prepared;
                assert activeTx == null;
                assert responseChannel != null;
                ControllerLogger.MGMT_OP_LOGGER.tracef("sending prepared response for %d  --- interrupted: %s", getOperationId(), Thread.currentThread().isInterrupted());
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
        }

        synchronized void completeTx(final ManagementRequestContext<ExecuteRequestContext> context, final boolean commit) {
            if (!prepared) {
                assert !commit; // can only be rollback before it's prepared

                ControllerLogger.MGMT_OP_LOGGER.tracef("completeTx (cancel unprepared) for %d", getOperationId());
                rollbackOnPrepare = true;
                cancel(context);

                // TODO response !?

            } else if (txCompleted) {
                // A 2nd call means a cancellation from the remote side after the tx was committed/rolled back
                // This would usually mean the completion of the request is hanging for some reason
                assert !commit; // can only be rollback if this has already been called
                ControllerLogger.MGMT_OP_LOGGER.tracef("completeTx (post-commit cancel) for %d", getOperationId());
                cancel(context);
            } else {
                assert activeTx != null;
                assert responseChannel == null;
                responseChannel = context;
                ControllerLogger.MGMT_OP_LOGGER.tracef("completeTx (%s) for %d", commit, getOperationId());
                if (commit) {
                    activeTx.commit();
                } else {
                    activeTx.rollback();
                }
                txCompleted = true;
                txCompletedLatch.countDown();
            }
        }

        synchronized void failed(final ModelNode response) {
            if(prepared) {
                // in case commit or rollback throws an exception, to conform with the API we still send an operation-completed message
                completed(response);
            } else {
                assert responseChannel != null;
                ControllerLogger.MGMT_OP_LOGGER.tracef("sending pre-prepare failed response for %d  --- interrupted: %s", getOperationId(), Thread.currentThread().isInterrupted());
                try {
                    // 2) send the operation-failed message (done)
                    sendResponse(responseChannel, ModelControllerProtocol.PARAM_OPERATION_FAILED, response);
                } catch (IOException e) {
                    ControllerLogger.MGMT_OP_LOGGER.debugf(e, "failed to process message");
                } finally {
                    getResultHandler().done(null);
                }
            }
        }

        synchronized void completed(final ModelNode response) {
            assert prepared;
            assert responseChannel != null;
            ControllerLogger.MGMT_OP_LOGGER.tracef("sending completed response for %d  --- interrupted: %s", getOperationId(), Thread.currentThread().isInterrupted());
            try {
                // 4) operation-completed (done)
                sendResponse(responseChannel, ModelControllerProtocol.PARAM_OPERATION_COMPLETED, response);
            } catch (IOException e) {
                ControllerLogger.MGMT_OP_LOGGER.debugf(e, "failed to process message");
            } finally {
                getResultHandler().done(null);
            }
        }

        private void cancel(final ManagementRequestContext<ExecuteRequestContext> context) {
            context.executeAsync(new ManagementRequestContext.AsyncTask<ExecuteRequestContext>() {
                @Override
                public void execute(ManagementRequestContext<ExecuteRequestContext> executeRequestContextManagementRequestContext) throws Exception {
                    operation.getResultHandler().cancel();
                }
            }, false);
        }

    }

    /**
     * Send an operation response.
     *
     * @param context the request context
     * @param responseType the response type
     * @param response the operation response
     * @throws java.io.IOException for any error
     */
    static void sendResponse(final ManagementRequestContext<ExecuteRequestContext> context, final byte responseType, final ModelNode response) throws IOException {

        // WFLY-3090 Protect the communication channel from getting closed due to administrative
        // cancellation of the management op by using a separate thread to send
        final CountDownLatch latch = new CountDownLatch(1);
        final IOExceptionHolder exceptionHolder = new IOExceptionHolder();
        context.executeAsync(new AsyncTask<TransactionalProtocolOperationHandler.ExecuteRequestContext>() {

            @Override
            public void execute(final ManagementRequestContext<ExecuteRequestContext> context) throws Exception {
                MGMT_OP_LOGGER.tracef("Transmitting response for %d", context.getOperationId());
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
                } catch (IOException toCache) {
                    exceptionHolder.exception = toCache;
                } finally {
                    StreamUtils.safeClose(output);
                    latch.countDown();
                }
            }
        }, false);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (exceptionHolder.exception != null) {
            throw exceptionHolder.exception;
        }
    }

    static Subject readSubject(final DataInput input) throws IOException {
        return SubjectProtocolUtil.read(input);
    }

    private static class IOExceptionHolder {
        private IOException exception;
    }

}
