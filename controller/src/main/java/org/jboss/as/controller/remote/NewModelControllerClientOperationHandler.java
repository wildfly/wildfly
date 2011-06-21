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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.old.ProtocolUtils;
import org.jboss.dmr.ModelNode;

/**
 * Operation handlers for the remote implementation of {@link org.jboss.as.controller.client.ModelControllerClient}
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class NewModelControllerClientOperationHandler extends NewAbstractModelControllerOperationHandler {

    private final Map<Integer, Thread> asynchRequests = Collections.synchronizedMap(new HashMap<Integer, Thread>());

    /**
     * @param executorService executor to use to execute requests from this operation handler to the initiator
     * @param controller the target controller
     */
    public NewModelControllerClientOperationHandler(final ExecutorService executorService, final NewModelController controller) {
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

        public ExecuteRequestHandler(boolean asynch) {
            this.asynch = asynch;
        }

        @Override
        protected void readRequest(final DataInput input) throws IOException {
            batchId = getContext().getHeader().getBatchId();
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_OPERATION);
            operation.readExternal(input);
            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAMS_LENGTH);
            attachmentsLength = input.readInt();
        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            try {
                ModelNode result;
                try {
                    log.tracef("Executing client request %d(%d)", batchId, getContext().getHeader().getRequestId());
                    if (asynch) {
                        //register the cancel handler
                        asynchRequests.put(batchId, Thread.currentThread());
                    }
                    result = controller.execute(
                            operation,
                            new OperationMessageHandlerProxy(getContext(), batchId),
                            NewModelController.OperationTransactionControl.COMMIT,
                            new OperationAttachmentsProxy(getContext(), batchId, attachmentsLength));
                } catch (Exception e) {
                    final ModelNode failure = new ModelNode();
                    failure.get(OUTCOME).set(FAILED);
                    failure.get(FAILURE_DESCRIPTION).set(e.getClass().getName() + ":" + e.getMessage());
                    result = failure;
                } finally {
                    log.tracef("Executed client request %d", batchId);
                }
                output.write(ModelControllerProtocol.PARAM_RESPONSE);
                result.writeExternal(output);
            } finally {
                if (asynch) {
                    asynchRequests.remove(batchId);
                }
            }
        }
    }

    private class CancelAsyncRequestHandler extends ManagementRequestHandler {
        private int batchId;
        @Override
        protected void readRequest(DataInput input) throws IOException {
            batchId = getContext().getHeader().getBatchId();
        }

        @Override
        protected void writeResponse(FlushableDataOutput output) throws IOException {
            Thread t = asynchRequests.get(batchId);
            if (t != null) {
                t.interrupt();
            }
            else {
                throw new IOException("No asynch request with batch id " + batchId);
            }
        }
    }
}
