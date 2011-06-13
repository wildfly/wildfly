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
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.client.NewModelControllerClient;
import org.jboss.as.controller.client.NewModelControllerProtocol;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.old.ProtocolUtils;
import org.jboss.dmr.ModelNode;

/**
 * Operation handlers for the remote implementation of {@link NewModelControllerClient}
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class NewModelControllerClientOperationHandler extends NewAbstractModelControllerOperationHandler {

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
        if (id == NewModelControllerProtocol.EXECUTE_CLIENT_REQUEST) {
            return new ExecuteRequestHandler();
        }
        return null;
    }

    /**
     * Handles incoming {@link _TempNewProxyController#execute(ModelNode, OperationMessageHandler, ProxyOperationControl, org.jboss.as.controller.client._TempNewOperationAttachments)}
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
        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            ModelNode result;
            try {
                result = controller.execute(
                        operation,
                        new OperationMessageHandlerProxy(getContext(), batchId),
                        NewModelController.OperationTransactionControl.COMMIT,
                        new OperationAttachmentsProxy(getContext(), batchId, attachmentsLength));
            } catch (Exception e) {
                e.printStackTrace();
                final ModelNode failure = new ModelNode();
                failure.get(OUTCOME).set(FAILED);
                failure.get(FAILURE_DESCRIPTION).set(e.getClass().getName() + ":" + e.getMessage());
                result = failure;
            }
            output.write(NewModelControllerProtocol.PARAM_RESPONSE);
            result.writeExternal(output);
        }
    }
}
