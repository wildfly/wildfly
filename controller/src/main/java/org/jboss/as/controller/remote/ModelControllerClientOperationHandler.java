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

import static org.jboss.as.controller.ControllerLogger.ROOT_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;

import java.io.DataInput;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.RequestProcessingException;
import org.jboss.as.protocol.mgmt.ProtocolUtils;
import org.jboss.dmr.ModelNode;

/**
 * Operation handlers for the remote implementation of {@link org.jboss.as.controller.client.ModelControllerClient}
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Emanuel Muckenhuber
 */
public class ModelControllerClientOperationHandler extends AbstractModelControllerOperationHandler {

    private final Map<Integer, AsyncRequestHandler> asynchRequests = Collections.synchronizedMap(new HashMap<Integer, AsyncRequestHandler>());

    /**
     * @param executorService executor to use to execute requests from this operation handler to the initiator
     * @param controller the target controller
     */
    public ModelControllerClientOperationHandler(final ExecutorService executorService, final ModelController controller) {
        super(executorService, controller);
    }

    /** {@inheritDoc} */
    @Override
    public ManagementRequestHandler getRequestHandler(final byte id) {
        if (id == ModelControllerProtocol.EXECUTE_CLIENT_REQUEST) {
            return new SyncRequestHandler();
        } else if (id == ModelControllerProtocol.EXECUTE_ASYNC_CLIENT_REQUEST) {
            return new AsyncRequestHandler();
        } else if (id == ModelControllerProtocol.CANCEL_ASYNC_REQUEST) {
            return new CancelAsyncRequestHandler();
        }
        return null;
    }

    abstract class ExecuteRequestHandler extends ManagementRequestHandler {
        private ModelNode operation = new ModelNode();
        private int batchId;
        private int attachmentsLength;

        @Override
        protected void readRequest(final DataInput input) throws IOException {
            batchId = getHeader().getBatchId();
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_OPERATION);
            operation.readExternal(input);
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAMS_LENGTH);
            attachmentsLength = input.readInt();
        }

        protected void processRequest() throws RequestProcessingException {
            final OperationAttachmentsProxy attachmentsProxy = new OperationAttachmentsProxy(getChannel(), batchId, attachmentsLength);
            try {
                ROOT_LOGGER.tracef("Executing client request %d(%d)", batchId, getHeader().getRequestId());
                doProcessRequest(batchId, operation, attachmentsProxy);
            } catch (final Exception e) {
                attachmentsProxy.shutdown(e);
            } finally {
                ROOT_LOGGER.tracef("Executed client request %d", batchId);
            }
        }

        /**
         * Do process the request.
         *
         * @param batchId the batch id
         * @param operation the operation
         * @param attachments the operation attachments
         */
        protected abstract void doProcessRequest(int batchId, ModelNode operation, OperationAttachments attachments);

    }

    /**
     * The sync request handler. Perhaps we don't even need this one? might not make any difference on the client side?
     */
    class SyncRequestHandler extends ExecuteRequestHandler {
        private ModelNode result;

        @Override
        protected void doProcessRequest(final int batchId, final ModelNode operation, final OperationAttachments attachments) {
            try {
                result = controller.execute(
                        operation,
                        new OperationMessageHandlerProxy(getChannel(), batchId),
                        ModelController.OperationTransactionControl.COMMIT,
                        attachments);
            } catch (Exception e) {
                final ModelNode failure = new ModelNode();
                failure.get(OUTCOME).set(FAILED);
                failure.get(FAILURE_DESCRIPTION).set(e.getClass().getName() + ":" + e.getMessage());
                result = failure;
            }
        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            output.write(ModelControllerProtocol.PARAM_RESPONSE);
            result.writeExternal(output);
        }

    }

    class AsyncRequestHandler extends ExecuteRequestHandler {
        private volatile boolean cancelled = false;
        private volatile Thread runner;

        @Override
        protected void doProcessRequest(final int batchId, final ModelNode operation, final OperationAttachments attachments) {
            asynchRequests.put(batchId, AsyncRequestHandler.this);
            // Execute the async in a different thread.
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    if(cancelled) {
                        return;
                    }
                    runner = Thread.currentThread();
                    try {
                        final ModelNode result = new ModelNode();
                        try {
                            result.set(controller.execute(
                                    operation,
                                    new OperationMessageHandlerProxy(getChannel(), batchId),
                                    ModelController.OperationTransactionControl.COMMIT,
                                    attachments));
                        } catch (Exception e) {
                            final ModelNode failure = new ModelNode();
                            failure.get(OUTCOME).set(FAILED);
                            failure.get(FAILURE_DESCRIPTION).set(e.getClass().getName() + ":" + e.getMessage());
                            result.set(failure);
                        }
                        // Write the message once the operation completes
                        writeMessageResponse(new ManagementWriteResponseCallback() {
                            @Override
                            public void writeResponse(final FlushableDataOutput output) throws IOException {
                                output.write(ModelControllerProtocol.PARAM_RESPONSE);
                                result.writeExternal(output);
                            }
                        });
                    } finally {
                        asynchRequests.remove(batchId);
                    }

                }
            });
        }

        @Override
        protected boolean isWriteResponse() {
            return false;
        }

        void cancel() {
            cancelled = true;
            Thread t = runner;
            if(t != null) {
                t.interrupt();
            }
        }

    }

    private class CancelAsyncRequestHandler extends ManagementRequestHandler {
        private int batchId;
        @Override
        protected void readRequest(DataInput input) throws IOException {
            batchId = getHeader().getBatchId();
        }

        protected void processRequest() throws RequestProcessingException {
            final AsyncRequestHandler handler = asynchRequests.remove(batchId);
            if (handler != null) {
                handler.cancel();
            } else {
                throw MESSAGES.asynchRequestNotFound(batchId);
            }
        }
    }

}
