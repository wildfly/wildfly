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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransaction;
import org.jboss.as.controller.ProxyController.ProxyOperationControl;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.ManagementResponseHandler;
import org.jboss.as.protocol.mgmt.RequestProcessingException;
import org.jboss.as.protocol.old.ProtocolUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.HandleableCloseable.Key;

/**
 * This model controller relies on the clients connecting with
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class TransactionalModelControllerOperationHandler extends AbstractModelControllerOperationHandler {

    private Map<Integer, ExecuteRequestContext> activeTransactions = Collections.synchronizedMap(new HashMap<Integer, TransactionalModelControllerOperationHandler.ExecuteRequestContext>());

    public TransactionalModelControllerOperationHandler(final ExecutorService executorService, final ModelController controller) {
        super(executorService, controller);
    }

    @Override
    public ManagementRequestHandler getRequestHandler(final byte id) {
        if (id == ModelControllerProtocol.EXECUTE_TX_REQUEST) {
            return new ExecuteRequestHandler();
        } else if (id == ModelControllerProtocol.COMPLETE_TX_REQUEST) {
            return new CompleteTxOperationHandler();
        } else if (id == ModelControllerProtocol.TEMP_PING_REQUEST){
            return new PingRequestHandler();
        }
        return null;
    }

    //TODO this should be deleted once REM3-121 is available
    private static class PingRequestHandler extends ManagementRequestHandler {
    }

    /**
     * Handles incoming {@link org.jboss.as.controller.ProxyController#execute(ModelNode, OperationMessageHandler, ProxyOperationControl, org.jboss.as.controller.client.OperationAttachments)}
     * requests from the remote proxy controller and forwards them to the target model controller
     */
    private class ExecuteRequestHandler extends ManagementRequestHandler {
        private ModelNode operation = new ModelNode();
        private int attachmentsLength;
        private ExecuteRequestContext executeRequestContext;

        @Override
        protected void readRequest(final DataInput input) throws IOException {
            executeRequestContext = new ExecuteRequestContext(getChannel(), getHeader().getBatchId());
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_OPERATION);
            operation.readExternal(input);
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAMS_LENGTH);
            attachmentsLength = input.readInt();
        }

        @Override
        protected void processRequest() throws RequestProcessingException {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    final OperationMessageHandlerProxy messageHandlerProxy = new OperationMessageHandlerProxy(getChannel(), executeRequestContext.getBatchId());
                    final ProxyOperationControlProxy control = new ProxyOperationControlProxy(executeRequestContext);
                    final OperationAttachmentsProxy attachmentsProxy = new OperationAttachmentsProxy(getChannel(), executeRequestContext.getBatchId(), attachmentsLength);
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
                        try {
                            executeRequestContext.awaitTxCompleted();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            executeRequestContext.setError("Error waiting for Tx commit/rollback");
                        }
                        control.operationCompleted(result);
                    }
                }
            });
            try {
                executeRequestContext.awaitPreparedOrFailed();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executeRequestContext.setError(e.getMessage());
                throw new RequestProcessingException("Thread was interrupted waiting for the operation to prepare/fail");
            }
        }
    }

    private class CompleteTxOperationHandler extends ManagementRequestHandler {
        byte commitOrRollback;
        @Override
        protected void readRequest(DataInput input) throws IOException {
            commitOrRollback = input.readByte();
        }

        @Override
        protected void processRequest() throws RequestProcessingException {
            ExecuteRequestContext executeRequestContext = activeTransactions.get(getHeader().getBatchId());
            if (executeRequestContext == null) {
                throw new RequestProcessingException("No active tx found for id " + getHeader().getBatchId());
            }
            executeRequestContext.setTxCompleted(commitOrRollback == ModelControllerProtocol.PARAM_COMMIT);
        }

        @Override
        protected void writeResponse(FlushableDataOutput output) throws IOException {
            super.writeResponse(output);
        }

    }

    /**
     * A proxy to the proxy operation control proxy on the remote caller
     */
    private class ProxyOperationControlProxy implements ProxyOperationControl {
        final ExecuteRequestContext executeRequestContext;

        public ProxyOperationControlProxy(final ExecuteRequestContext executeRequestContext) {
            this.executeRequestContext = executeRequestContext;
        }

        @Override
        public void operationPrepared(final OperationTransaction transaction, final ModelNode result) {
            try {
                new OperationStatusRequest(executeRequestContext.getBatchId(), result) {

                    @Override
                    protected byte getRequestCode() {
                        return ModelControllerProtocol.OPERATION_PREPARED_REQUEST;
                    }

                    @Override
                    protected void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
                        //TODO register Tx
                        executeRequestContext.setActiveTX(transaction);
                        activeTransactions.put(executeRequestContext.getBatchId(), executeRequestContext);
                        super.writeRequest(protocolVersion, output);
                    }

                    protected ManagementResponseHandler<Void> getResponseHandler() {
                        return new ManagementResponseHandler<Void>() {
                            @Override
                            protected Void readResponse(DataInput input) throws IOException {
                                return null;
                            }
                        };
                    }

                }.executeForResult(executorService, getChannelStrategy(executeRequestContext.getChannel()));
                executeRequestContext.setPreparedOrFailed();
            } catch (Exception e) {
                executeRequestContext.setError(e.getMessage());
                throw new RuntimeException(e);
            }

            try {
                executeRequestContext.awaitTxCompleted();
            } catch (InterruptedException e) {
                executeRequestContext.setError("Interrupted while waiting for request");
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void operationFailed(final ModelNode response) {
            try {
                new OperationStatusRequest(executeRequestContext.getBatchId(), response) {

                    @Override
                    protected byte getRequestCode() {
                        return ModelControllerProtocol.OPERATION_FAILED_REQUEST;
                    }

                }.executeForResult(executorService, getChannelStrategy(executeRequestContext.getChannel()));
            } catch (Exception e) {
                executeRequestContext.setError(e.getMessage());
                throw new RuntimeException(e);
            }
            executeRequestContext.setPreparedOrFailed();
        }

        @Override
        public void operationCompleted(final ModelNode response) {
            new OperationStatusRequest(executeRequestContext.getBatchId(), response) {

                @Override
                protected byte getRequestCode() {
                    return ModelControllerProtocol.OPERATION_COMPLETED_REQUEST;
                }

            }.execute(executorService, getChannelStrategy(executeRequestContext.getChannel()));
        }

        /**
         * Base class for sending operation status reports back to the remote caller
         */
        private abstract class OperationStatusRequest extends ManagementRequest<Void> {
            private final ModelNode response;

            public OperationStatusRequest(final int batchId, final ModelNode response) {
                super(batchId);
                this.response = response;
            }
            @Override
            protected void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
                output.write(ModelControllerProtocol.PARAM_RESPONSE);
                response.writeExternal(output);
            }

            protected ManagementResponseHandler<Void> getResponseHandler() {
                return ManagementResponseHandler.EMPTY_RESPONSE;
            }

        }
    }

    private class ExecuteRequestContext {
        final ManagementChannel channel;
        final int batchId;
        final CountDownLatch preparedOrFailedLatch = new CountDownLatch(1);
        final CountDownLatch txCompletedLatch = new CountDownLatch(1);
        final Key closableKey;
        volatile OperationTransaction activeTx;
        volatile String error;

        public ExecuteRequestContext(final ManagementChannel channel, final int batchId) {
            this.channel = channel;
            this.batchId = batchId;
            closableKey = channel.addCloseHandler(new CloseHandler<Channel>() {
                public void handleClose(final Channel closed, final IOException exception) {
                    setError("Channel Closed");
                }
            });
        }

        ManagementChannel getChannel() {
            return channel;
        }

        int getBatchId() {
            return batchId;
        }

        void awaitPreparedOrFailed() throws InterruptedException {
            preparedOrFailedLatch.await();
        }

        void setPreparedOrFailed() {
            preparedOrFailedLatch.countDown();
            closableKey.remove();
        }

        void setActiveTX(OperationTransaction tx) {
            this.activeTx = tx;
        }

        void awaitTxCompleted() throws InterruptedException {
            txCompletedLatch.await();
            if (error != null) {
                throw new RuntimeException(error);
            }
        }

        void setTxCompleted(boolean commit) {
            if (commit) {
                activeTx.commit();
            } else {
                activeTx.rollback();
            }
            txCompletedLatch.countDown();
            if (error != null) {
                throw new RuntimeException(error);
            }
        }

        synchronized void setError(String error) {
            this.error = error;
            activeTransactions.remove(batchId);
            preparedOrFailedLatch.countDown();
            txCompletedLatch.countDown();
        }

    }
}
