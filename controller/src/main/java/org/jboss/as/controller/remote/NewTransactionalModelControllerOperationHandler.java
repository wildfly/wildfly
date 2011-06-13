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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.NewModelController.OperationTransaction;
import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.NewProxyController.ProxyOperationControl;
import org.jboss.as.controller.client.NewModelControllerProtocol;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
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

    public NewTransactionalModelControllerOperationHandler(final ExecutorService executorService, final NewModelController controller) {
        super(executorService, controller);
    }

    @Override
    public ManagementRequestHandler getRequestHandler(final byte id) {
        if (id == NewModelControllerProtocol.EXECUTE_TX_REQUEST) {
            return new ExecuteRequestHandler();
        }
        return null;
    }

    /**
     * Handles incoming {@link NewProxyController#execute(ModelNode, OperationMessageHandler, ProxyOperationControl, org.jboss.as.controller.client.OperationAttachments)}
     * requests from the remote proxy controller and forwards them to the target model controller
     */
    private class ExecuteRequestHandler extends ManagementRequestHandler {
        private ModelNode operation = new ModelNode();
        private int batchId;
        private int attachmentsLength;

        @Override
        protected void readRequest(final DataInput input) throws IOException {
            batchId = getContext().getHeader().getBatchId();
            ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_OPERATION);
            operation.readExternal(input);
            ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_INPUTSTREAMS_LENGTH);
            attachmentsLength = input.readInt();

            //TODO make sure this is only added once
            getContext().getChannel().addCloseHandler(new CloseHandler<Channel>() {
                @Override
                public void handleClose(Channel closed) {
                }
            });

        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            final CountDownLatch preparedOrFailedLatch = new CountDownLatch(1);
            final CountDownLatch txCompletedLatch = new CountDownLatch(1);
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    final OperationMessageHandlerProxy messageHandlerProxy = new OperationMessageHandlerProxy(getContext(), batchId);
                    final ProxyOperationControlProxy control = new ProxyOperationControlProxy(getContext(), batchId, preparedOrFailedLatch, txCompletedLatch);
                    final ModelNode result;
                    try {
                        result = controller.execute(
                                operation,
                                messageHandlerProxy,
                                control,
                                new OperationAttachmentsProxy(getContext(), batchId, attachmentsLength));
                    } catch (Exception e) {
                        final ModelNode failure = new ModelNode();
                        failure.get(OUTCOME).set(FAILED);
                        failure.get(FAILURE_DESCRIPTION).set(e.getClass().getName() + ":" + e.getMessage());
                        control.operationFailed(failure);
                        return;
                    }
                    if (result.hasDefined(FAILURE_DESCRIPTION)) {
                        control.operationFailed(result);
                    } else {
                        try {
                            txCompletedLatch.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        control.operationCompleted(result);
                    }
                }
            });
            try {
                preparedOrFailedLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Thread was interrupted waiting for the operation to prepare/fail");
            }
        }
    }

    /**
     * A proxy to the proxy operation control proxy on the remote caller
     */
    private class ProxyOperationControlProxy implements ProxyOperationControl {
        final ManagementRequestContext context;
        final int batchId;
        final CountDownLatch preparedOrFailedLatch;
        final CountDownLatch txCompletedLatch;

        public ProxyOperationControlProxy(final ManagementRequestContext context, final int batchId, final CountDownLatch preparedOrFailedLatch, final CountDownLatch txCompletedLatch) {
            this.context = context;
            this.batchId = batchId;
            this.preparedOrFailedLatch = preparedOrFailedLatch;
            this.txCompletedLatch = txCompletedLatch;
        }

        @Override
        public void operationPrepared(final OperationTransaction transaction, final ModelNode result) {
            try {
                new OperationStatusRequest(batchId, result) {

                    @Override
                    protected byte getRequestCode() {
                        return NewModelControllerProtocol.OPERATION_PREPARED_REQUEST;
                    }

                    @Override
                    protected void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
                        super.writeRequest(protocolVersion, output);
                    }

                    @Override
                    protected Void readResponse(DataInput input) throws IOException {
                        //The caller has delegated the operationPrepared() call
                        ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_PREPARED);
                        preparedOrFailedLatch.countDown();

                        //Now check if the Tx was committed or rolled back
                        byte status = input.readByte();
                        if (status == NewModelControllerProtocol.PARAM_COMMIT) {
                            transaction.commit();
                            txCompletedLatch.countDown();

                        } else if (status == NewModelControllerProtocol.PARAM_ROLLBACK){
                            transaction.rollback();
                            txCompletedLatch.countDown();
                        } else {
                            throw new IllegalArgumentException("Invalid status code " + status);
                        }

                        return null;
                    }

                }.executeForResult(executorService, getChannelStrategy(context.getChannel()));
            } catch (Exception e) {
                // AutoGenerated
                throw new RuntimeException(e);
            }
        }

        @Override
        public void operationFailed(final ModelNode response) {
            try {
                new OperationStatusRequest(batchId, response) {

                    @Override
                    protected byte getRequestCode() {
                        return NewModelControllerProtocol.OPERATION_FAILED_REQUEST;
                    }

                }.executeForResult(executorService, getChannelStrategy(context.getChannel()));
            } catch (Exception e) {
                // AutoGenerated
                throw new RuntimeException(e);
            }
            preparedOrFailedLatch.countDown();
        }

        @Override
        public void operationCompleted(final ModelNode response) {
            new OperationStatusRequest(batchId, response) {

                @Override
                protected byte getRequestCode() {
                    return NewModelControllerProtocol.OPERATION_COMPLETED_REQUEST;
                }

            }.execute(executorService, getChannelStrategy(context.getChannel()));
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
