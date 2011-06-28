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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.RequestProcessingException;
import org.jboss.as.protocol.old.ProtocolUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.HandleableCloseable.Key;

/**
 * Operation handlers for the remote implementation of {@link org.jboss.as.controller.client.ModelControllerClient}
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ModelControllerClientOperationHandler extends AbstractModelControllerOperationHandler {

    private final Map<Integer, Thread> asynchRequests = Collections.synchronizedMap(new HashMap<Integer, Thread>());

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
            return new ExecuteRequestHandler(false);
        } else if (id == ModelControllerProtocol.EXECUTE_ASYNC_CLIENT_REQUEST) {
            return new ExecuteRequestHandler(true);
        } else if (id == ModelControllerProtocol.CANCEL_ASYNC_REQUEST) {
            return new CancelAsyncRequestHandler();
        }
        return null;
    }

    /**
     * Handles incoming {@link _TempNewProxyController#execute(ModelNode, OperationMessageHandler, ProxyOperationControl, org.jboss.as.controller.client._TempNewOperationAttachments)}
     * requests from the remote proxy controller and forwards them to the target model controller
     */
    private class ExecuteRequestHandler extends ManagementRequestHandler {
        private final boolean asynch;
        private ModelNode operation = new ModelNode();
        private int batchId;
        private int attachmentsLength;
        private ModelNode result;

        public ExecuteRequestHandler(boolean asynch) {
            this.asynch = asynch;
        }

        @Override
        protected void readRequest(final DataInput input) throws IOException {
            batchId = getHeader().getBatchId();
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_OPERATION);
            operation.readExternal(input);
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAMS_LENGTH);
            attachmentsLength = input.readInt();
        }

        protected void processRequest() throws RequestProcessingException {
            if (!asynch) {
                doProcessRequest();
            } else {
                //Do asynchrounous invocations in a separate thread to avoid interruption of the thread
                //if cancelled filtering up to the NIO layer which results in ClosedByInterruptException
                //which closes the channel
                Future<Void> future = executorService.submit(new Callable<Void>() {
                    public Void call() throws Exception {
                        doProcessRequest();
                        return null;
                    }
                });
                try {
                    future.get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RequestProcessingException) {
                        throw (RequestProcessingException)cause;
                    }
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException)cause;
                    }
                    throw new RequestProcessingException(cause);
                } catch (InterruptedException e) {
                    Thread t = asynchRequests.get(batchId);
                    if (t != null) {
                        t.interrupt();
                    }
                    Thread.currentThread().interrupt();
                    throw new RequestProcessingException("Thread was interrupted waiting for a response for asynch operation");
                }
            }
        }

        private void doProcessRequest() {
            final Key closeKey = getChannel().addCloseHandler(new CloseHandler<Channel>() {
                public void handleClose(final Channel closed, final IOException exception) {
                    asynchRequests.remove(getHeader().getBatchId());
                }
            });
            OperationAttachmentsProxy attachmentsProxy = new OperationAttachmentsProxy(getChannel(), batchId, attachmentsLength);
            try {
                try {
                    log.tracef("Executing client request %d(%d)", batchId, getHeader().getRequestId());
                    if (asynch) {
                        //register the cancel handler
                        asynchRequests.put(batchId, Thread.currentThread());
                    }
                    result = controller.execute(
                            operation,
                            new OperationMessageHandlerProxy(getChannel(), batchId),
                            ModelController.OperationTransactionControl.COMMIT,
                            attachmentsProxy);
                } catch (Exception e) {
                    final ModelNode failure = new ModelNode();
                    failure.get(OUTCOME).set(FAILED);
                    failure.get(FAILURE_DESCRIPTION).set(e.getClass().getName() + ":" + e.getMessage());
                    result = failure;
                    attachmentsProxy.shutdown(e);
                } finally {
                    log.tracef("Executed client request %d", batchId);
                }
            } finally {
                if (asynch) {
                    asynchRequests.remove(batchId);
                }
                closeKey.remove();
            }
        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            output.write(ModelControllerProtocol.PARAM_RESPONSE);
            result.writeExternal(output);
        }
    }

    private class CancelAsyncRequestHandler extends ManagementRequestHandler {
        private int batchId;
        @Override
        protected void readRequest(DataInput input) throws IOException {
            batchId = getHeader().getBatchId();
        }

        protected void processRequest() throws RequestProcessingException {
            Thread t = asynchRequests.get(batchId);
            if (t != null) {
                t.interrupt();
            }
            else {
                throw new RequestProcessingException("No asynch request with batch id " + batchId);
            }
        }
    }
}
