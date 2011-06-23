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

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransaction;
import org.jboss.as.controller.ProxyController.ProxyOperationControl;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.old.ProtocolUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.HandleableCloseable.Key;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class TransactionalModelControllerOperationHandler extends AbstractModelControllerOperationHandler {

    public TransactionalModelControllerOperationHandler(final ExecutorService executorService, final ModelController controller) {
        super(executorService, controller);
    }

    @Override
    public ManagementRequestHandler getRequestHandler(final byte id) {
        if (id == ModelControllerProtocol.EXECUTE_TX_REQUEST) {
            return new ExecuteRequestHandler();
        } else if (id == ModelControllerProtocol.TEMP_PING_REQUEST){
            return new PingRequestHandler();
        }
        return null;
    }

    //TODO this should be deleted once REM3-121 is available
    private static class PingRequestHandler extends ManagementRequestHandler {

        @Override
        protected void readRequest(DataInput input) throws IOException {
        }

        @Override
        protected void writeResponse(FlushableDataOutput output) throws IOException {
        }
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
            executeRequestContext = new ExecuteRequestContext(getContext());
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_OPERATION);
            operation.readExternal(input);
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAMS_LENGTH);
            attachmentsLength = input.readInt();
        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    final OperationMessageHandlerProxy messageHandlerProxy = new OperationMessageHandlerProxy(getContext(), executeRequestContext.getBatchId());
                    final ProxyOperationControlProxy control = new ProxyOperationControlProxy(executeRequestContext);
                    final OperationAttachmentsProxy attachmentsProxy = new OperationAttachmentsProxy(getContext(), executeRequestContext.getBatchId(), attachmentsLength);
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
                throw new IOException("Thread was interrupted waiting for the operation to prepare/fail");
            }
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
                        super.writeRequest(protocolVersion, output);
                    }

                    @Override
                    protected Void readResponse(DataInput input) throws IOException {
                        //The caller has delegated the operationPrepared() call
                        ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_PREPARED);
                        executeRequestContext.setPreparedOrFailed();

                        //Now check if the Tx was committed or rolled back
                        byte status = input.readByte();
                        if (status == ModelControllerProtocol.PARAM_COMMIT) {
                            transaction.commit();
                            executeRequestContext.setTxCompleted();

                        } else if (status == ModelControllerProtocol.PARAM_ROLLBACK){
                            transaction.rollback();
                            executeRequestContext.setTxCompleted();
                        } else {
                            throw new IllegalArgumentException("Invalid status code " + status);
                        }

                        return null;
                    }

                }.executeForResult(executorService, getChannelStrategy(executeRequestContext.getChannel()));
            } catch (Exception e) {
                executeRequestContext.setError(e.getMessage());
                throw new RuntimeException(e);
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

            @Override
            protected Void readResponse(final DataInput input) throws IOException {
                return null;
            }
        }
    }

    private static class ExecuteRequestContext {
        final ManagementRequestContext managementRequestContext;
        final CountDownLatch preparedOrFailedLatch = new CountDownLatch(1);
        final CountDownLatch txCompletedLatch = new CountDownLatch(1);
        final Key closableKey;
        volatile String error;

        public ExecuteRequestContext(final ManagementRequestContext managementRequestContext) {
            this.managementRequestContext = managementRequestContext;
            closableKey = managementRequestContext.getChannel().addCloseHandler(new CloseHandler<Channel>() {
                public void handleClose(Channel closed) {
                    setError("Channel Closed");
                }
            });
        }

        ManagementChannel getChannel() {
            return managementRequestContext.getChannel();
        }

        int getBatchId() {
            return managementRequestContext.getHeader().getBatchId();
        }

        void awaitPreparedOrFailed() throws InterruptedException {
            preparedOrFailedLatch.await();
        }

        void setPreparedOrFailed() {
            preparedOrFailedLatch.countDown();
            closableKey.remove();
        }

        void awaitTxCompleted() throws InterruptedException {
            txCompletedLatch.await();
            if (error != null) {
                throw new RuntimeException(error);
            }
        }

        void setTxCompleted() {
            txCompletedLatch.countDown();
            if (error != null) {
                throw new RuntimeException(error);
            }
        }

        synchronized void setError(String error) {
            this.error = error;
            preparedOrFailedLatch.countDown();
            txCompletedLatch.countDown();
        }

    }
}
