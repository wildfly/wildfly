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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;

import java.io.DataInput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.NewModelController.OperationTransaction;
import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.NewProxyController.ProxyOperationControl;
import org.jboss.as.controller.client.NewModelControllerProtocol;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.protocol.ProtocolChannel;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
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
public class NewTransactionalModelControllerOperationHandler extends NewAbstractModelControllerOperationHandler {

    private Map<Integer, OperationTransaction> activeTransactions = new HashMap<Integer, NewModelController.OperationTransaction>();

    public NewTransactionalModelControllerOperationHandler(final ExecutorService executorService, final NewModelController controller) {
        super(executorService, controller);
    }

    @Override
    public ManagementRequestHandler getRequestHandler(final byte id) {
        if (id == NewModelControllerProtocol.EXECUTE_REQUEST) {
            return new ExecuteRequestHandler();
        } else if (id == NewModelControllerProtocol.COMMIT_TRANSACTION_REQUEST) {
            return new CommitOperationTransactionRequestHandler();
        } else if (id == NewModelControllerProtocol.ROLLBACK_TRANSACTION_REQUEST) {
            return new RollbackOperationTransactionRequestHandler();
        }
        return null;
    }

    /**
     * Handles incoming {@link NewProxyController#execute(ModelNode, OperationMessageHandler, ProxyOperationControl, org.jboss.as.controller.client.OperationAttachments)}
     * requests from the remote proxy controller and forwards them to the target model controller
     */
    private class ExecuteRequestHandler extends ManagementRequestHandler {
        private ModelNode operation = new ModelNode();
        private int executionId;
        private int attachmentsLength;

        @Override
        protected void readRequest(final DataInput input) throws IOException {
            ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_EXECUTION_ID);
            executionId = input.readInt();
            ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_OPERATION);
            operation.readExternal(input);
            ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_INPUTSTREAMS_LENGTH);
            attachmentsLength = input.readInt();

            //TODO make sure this is only added once
            getChannel().addCloseHandler(new CloseHandler<Channel>() {
                @Override
                public void handleClose(Channel closed) {
                    synchronized (activeTransactions) {
                        for (OperationTransaction tx : activeTransactions.values()) {
                            tx.rollback();
                        }
                    }
                }
            });

        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    ProxyOperationControlProxy control = new ProxyOperationControlProxy(getChannel(), executionId);
                    final ModelNode result;
                    try {
                        result = controller.execute(
                                operation,
                                new OperationMessageHandlerProxy(getChannel(), executionId),
                                control,
                                new OperationAttachmentsProxy(getChannel(), executionId, attachmentsLength));
                    } catch (Exception e) {
                        activeTransactions.remove(executionId);
                        final ModelNode failure = new ModelNode();
                        failure.get(OUTCOME).set(FAILED);
                        failure.get(FAILURE_DESCRIPTION).set(e.getClass().getName() + ":" + e.getMessage());
                        control.operationFailed(failure);
                        return;
                    }
                    if (result.hasDefined(OUTCOME) && result.get(OUTCOME).asString().equals(FAILED)) {
                        activeTransactions.remove(executionId);
                        control.operationFailed(result);
                    } else {
                        control.operationCompleted(result);
                    }
                }
            });
        }
    }

    /**
     * Handles incoming {@link NewModelController.OperationTransaction} requests from the remote proxy controller and forwards them to the target
     * model controller
     */
    private abstract class OperationTransactionRequestHandler extends ManagementRequestHandler {

        @Override
        protected void readRequest(final DataInput input) throws IOException {
            ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_EXECUTION_ID);
            int executionId = input.readInt();
            OperationTransaction tx = activeTransactions.remove(executionId);
            if (tx == null) {
                throw new IOException("No active transaction with id " + executionId);
            }
            handle(tx);
        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
        }

        protected abstract void handle(OperationTransaction tx);
    }

    /**
     * Handles incoming {@link NewModelController.OperationTransaction#commit()} requests from the remote proxy controller and forwards them to the target
     * model controller
     */
    private class CommitOperationTransactionRequestHandler extends OperationTransactionRequestHandler {

        @Override
        protected void handle(OperationTransaction tx) {
            tx.commit();
        }
    }

    /**
     * Handles incoming {@link NewModelController.OperationTransaction#rollback} requests from the remote proxy controller and forwards them to the target
     * model controller
     */
    private class RollbackOperationTransactionRequestHandler extends OperationTransactionRequestHandler {

        @Override
        protected void handle(OperationTransaction tx) {
            tx.rollback();
        }
    }

    /**
     * A proxy to the proxy operation control proxy on the remote caller
     */
    private class ProxyOperationControlProxy implements ProxyOperationControl {
        final ProtocolChannel channel;
        final int executionId;

        public ProxyOperationControlProxy(final ProtocolChannel channel, final int executionId) {
            this.channel = channel;
            this.executionId = executionId;
        }

        @Override
        public void operationPrepared(final OperationTransaction transaction, final ModelNode result) {
            final CountDownLatch completedLatch = new CountDownLatch(1);
            //Operation prepared should not return until the Tx has been committed or rolled back
            final OperationTransaction blockingTransaction = new OperationTransaction() {

                @Override
                public void rollback() {
                    transaction.rollback();
                    completedLatch.countDown();
                }

                @Override
                public void commit() {
                    transaction.commit();
                    completedLatch.countDown();
                }
            };
            activeTransactions.put(executionId, blockingTransaction);
            new OperationStatusRequest(executionId, result) {

                @Override
                protected byte getRequestCode() {
                    return NewModelControllerProtocol.OPERATION_PREPARED_REQUEST;
                }

            }.execute(executorService, getChannelStrategy(channel));
        }

        @Override
        public void operationFailed(final ModelNode response) {
            activeTransactions.remove(executionId);
            new OperationStatusRequest(executionId, response) {

                @Override
                protected byte getRequestCode() {
                    return NewModelControllerProtocol.OPERATION_FAILED_REQUEST;
                }

            }.execute(executorService, getChannelStrategy(channel));
        }

        @Override
        public void operationCompleted(final ModelNode response) {
            new OperationStatusRequest(executionId, response) {

                @Override
                protected byte getRequestCode() {
                    return NewModelControllerProtocol.OPERATION_COMPLETED_REQUEST;
                }

            }.execute(executorService, getChannelStrategy(channel));
        }

        /**
         * Base class for sending operation status reports back to the remote caller
         */
        private abstract class OperationStatusRequest extends ManagementRequest<Void> {
            private final int executionId;
            private final ModelNode response;

            public OperationStatusRequest(final int executionId, final ModelNode response) {
                this.executionId = executionId;
                this.response = response;
            }
            @Override
            protected void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
                output.write(NewModelControllerProtocol.PARAM_EXECUTION_ID);
                output.writeInt(executionId);
                output.write(NewModelControllerProtocol.PARAM_RESPONSE);
                response.writeExternal(output);
            }

            @Override
            protected Void readResponse(final DataInput input) throws IOException {
                return null;
            }
        }
    }
}
