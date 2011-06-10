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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.NewModelController.OperationTransaction;
import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.NewModelControllerProtocol;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.as.protocol.mgmt.ManagementChannelFactory;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestExecutionCallback;
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
public class NewRemoteProxyController implements NewProxyController, ManagementOperationHandler {

    private final PathAddress pathAddress;
    private final ManagementChannel channel;
    private final ExecutorService executorService;
    private final Map<Integer, ExecuteRequestContext> activeRequests = Collections.synchronizedMap(new HashMap<Integer, ExecuteRequestContext>());

    private NewRemoteProxyController(final ExecutorService executorService, final PathAddress pathAddress, final ManagementChannel channel) {
        this.pathAddress = pathAddress;
        this.channel = channel;
        this.executorService = executorService;

        channel.addCloseHandler(new CloseHandler<Channel>() {

            @Override
            public void handleClose(Channel closed) {
                activeRequests.clear();
            }
        });
    }

    /**
     * Creates a new remote proxy controller using an exisiting channel
     *
     * @param executorService the executor to use for the requests
     * @param pathAddress the address within the model of the created proxy controller
     * @param channel the channel to use for communication
     * @return the proxy controller
     */
    public static NewRemoteProxyController create(final ExecutorService executorService, final PathAddress pathAddress, final ManagementChannel channel) {
        return new NewRemoteProxyController(executorService, pathAddress, channel);
    }

    /**
     * Creates a new remote proxy controller connecting to a remote server
     *
     * @param executorService the executor to use for the requests
     * @param pathAddress the address within the model of the created proxy controller
     * @param hostName the host name of the remote server
     * @param port the port of the remote server
     * @param channelName the channel name
     * @return the proxy controller
     * @throws IOException if an error occurred
     * @throws ConnectException if we could not connect to the remote server
     */
    public static NewRemoteProxyController create(final ExecutorService executorService, final PathAddress pathAddress, final String hostName, final int port, String channelName) throws IOException {
        final ProtocolChannelClient<ManagementChannel> client;
        try {
            final ProtocolChannelClient.Configuration<ManagementChannel> configuration = new ProtocolChannelClient.Configuration<ManagementChannel>();
            configuration.setEndpointName("endpoint");
            configuration.setUriScheme("remote");
            configuration.setUri(new URI("remote://" + hostName +  ":" + port));
            configuration.setExecutor(executorService);
            configuration.setChannelFactory(new ManagementChannelFactory());
            client = ProtocolChannelClient.create(configuration);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        client.connect();

        ManagementChannel channel ;
        try {
            channel = client.openChannel(channelName);
        } catch (IOException e) {
            client.close();
            throw e;
        }
        channel.startReceiving();

        return new NewRemoteProxyController(executorService, pathAddress, channel);
    }

    /** {@inheritDoc} */
    @Override
    public PathAddress getProxyNodeAddress() {
        return pathAddress;
    }

    /** {@inheritDoc} */
    @Override
    public ManagementRequestHandler getRequestHandler(final byte id) {
        if (id == NewModelControllerProtocol.HANDLE_REPORT_REQUEST) {
            return new HandleReportRequestHandler();
        } else if (id == NewModelControllerProtocol.OPERATION_FAILED_REQUEST) {
            return new OperationFailedRequestHandler();
        } else if (id == NewModelControllerProtocol.OPERATION_COMPLETED_REQUEST) {
            return new OperationCompletedRequestHandler();
        } else if (id == NewModelControllerProtocol.OPERATION_PREPARED_REQUEST) {
            return new OperationPreparedRequestHandler();
        } else if (id == NewModelControllerProtocol.GET_INPUTSTREAM_REQUEST) {
            return new ReadAttachmentInputStreamRequestHandler();
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ModelNode operation, final OperationMessageHandler handler, final ProxyOperationControl control, final OperationAttachments attachments) {
        //As per the interface javadoc this method should block until either the operationFailed() or the operationPrepared() methods of the ProxyOperationControl have been called
        ExecuteRequest request = new ExecuteRequest(
            operation,
            handler,
            new ProxyOperationControl() {
                @Override
                public void operationPrepared(OperationTransaction transaction, ModelNode result) {
                    control.operationPrepared(transaction, result);
                }

                @Override
                public void operationFailed(ModelNode response) {
                    control.operationFailed(response);
                }

                @Override
                public void operationCompleted(ModelNode response) {
                    control.operationCompleted(response);
                }
            },
            attachments);
        try {
            request.executeForResult(executorService, getChannelStrategy());
        } catch (Exception e) {
            // AutoGenerated
            throw new RuntimeException(e);
        }
    }

    private ManagementClientChannelStrategy getChannelStrategy() {
        return ManagementClientChannelStrategy.create(channel);
    }

    /**
     * Propagates an execute() call from this proxy controller to the remote target controller
     */
    private class ExecuteRequest extends ManagementRequest<Void> {

        private final ExecuteRequestContext executeRequestContext;
        private final ModelNode operation;

        ExecuteRequest(final ModelNode operation, final OperationMessageHandler messageHandler, final ProxyOperationControl control, final OperationAttachments attachments) {
            super(true);
            this.operation = operation;
            executeRequestContext = new ExecuteRequestContext(messageHandler, control, attachments, channel);
        }

        @Override
        protected byte getRequestCode() {
            return NewModelControllerProtocol.EXECUTE_REQUEST;
        }

        @Override
        protected void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
            boolean success = false;
            activeRequests.put(getCurrentRequestId(), executeRequestContext);
            executeRequestContext.getExecutionCallback().registerOutgoingExecution(getCurrentRequestId());
            try {
                output.write(NewModelControllerProtocol.PARAM_OPERATION);
                operation.writeExternal(output);
                output.write(NewModelControllerProtocol.PARAM_INPUTSTREAMS_LENGTH);
                int inputStreamLength = 0;
                if (executeRequestContext.getAttachments() != null) {
                    List<InputStream> streams = executeRequestContext.getAttachments().getInputStreams();
                    if (streams != null) {
                        inputStreamLength = streams.size();
                    }
                }
                output.writeInt(inputStreamLength);
                success = true;
            } finally {
                if (!success) {
                    executeRequestContext.getExecutionCallback().completeOutgoingExecution(getCurrentRequestId());
                }
            }
        }

        @Override
        protected Void readResponse(final DataInput input) throws IOException {
            //TODO this needs cleaning up once the operation has been executed
            //activeRequests.remove(currentRequestId);
            return null;
        }
    }

    /**
     * Handles {@link OperationMessageHandler#handleReport(org.jboss.as.controller.client.MessageSeverity, String)} calls
     * done in the remote target controller
     */
    private class HandleReportRequestHandler extends ManagementRequestHandler {

        @Override
        protected void readRequest(final DataInput input) throws IOException {
            int executionId = getContext().getHeader().getExecutionId();
            ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_MESSAGE_SEVERITY);
            MessageSeverity severity = Enum.valueOf(MessageSeverity.class, input.readUTF());
            ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_MESSAGE);
            String message = input.readUTF();

            ExecuteRequestContext requestContext = activeRequests.get(executionId);
            if (requestContext == null) {
                throw new IOException("No active request found for " + executionId);
            }
            requestContext.getMessageHandler().handleReport(severity, message);
        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
        }
    }

    /**
     * Handles reads on the inputstreams returned by {@link OperationAttachments#getInputStreams()}
     * done in the remote target controller
     */
    private class ReadAttachmentInputStreamRequestHandler extends ManagementRequestHandler {
        InputStream attachmentInput;
        @Override
        protected void readRequest(final DataInput input) throws IOException {
            int executionId = getContext().getHeader().getExecutionId();
            ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_INPUTSTREAM_INDEX);
            int index = input.readInt();

            ExecuteRequestContext requestContext = activeRequests.get(executionId);
            if (requestContext == null) {
                throw new IOException("No active request found for " + executionId);
            }
            InputStream in = requestContext.getAttachments().getInputStreams().get(index);
            attachmentInput = in != null ? new BufferedInputStream(in) : null;
        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            if (attachmentInput != null) {
                int i = attachmentInput.read();
                while (i != -1) {
                    bout.write(i);
                    i = attachmentInput.read();
                }
            }
            byte[] bytes = bout.toByteArray();
            output.write(NewModelControllerProtocol.PARAM_INPUTSTREAM_LENGTH);
            output.writeInt(bytes.length);
            output.write(NewModelControllerProtocol.PARAM_INPUTSTREAM_CONTENTS);
            output.write(bytes);
        }
    }

    /**
     * Base class for handling {@link ProxyOperationControl} method calls done in the remote target controller
     */
    private abstract class ProxyOperationControlRequestHandler extends ManagementRequestHandler {
        @Override
        protected void readRequest(final DataInput input) throws IOException {
            int executionId = getContext().getHeader().getExecutionId();
            ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_RESPONSE);
            ModelNode response = new ModelNode();
            response.readExternal(input);

            ExecuteRequestContext requestContext = activeRequests.get(executionId);
            if (requestContext == null) {
                throw new IOException("No active request found for " + executionId);
            }
            handle(executionId, requestContext.getControl(), response);
        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
        }

        abstract void handle(final int executionId, final ProxyOperationControl control, final ModelNode response) throws IOException;
    }

    /**
     * Handles {@link ProxyOperationControl#operationFailed(ModelNode)} method calls done in the remote target controller
     */
    private class OperationFailedRequestHandler extends ProxyOperationControlRequestHandler {

        @Override
        void handle(final int executionId, final ProxyOperationControl control, final ModelNode response) {
            control.operationFailed(response);
            ExecuteRequestContext context = activeRequests.remove(executionId);
            context.getExecutionCallback().completeOutgoingExecution(executionId);
        }
    }

    /**
     * Handles {@link ProxyOperationControl#operationFailed(ModelNode)} method calls done in the remote target controller
     */
    private class OperationCompletedRequestHandler extends ProxyOperationControlRequestHandler {

        @Override
        void handle(final int executionId, final ProxyOperationControl control, final ModelNode response) {
            control.operationCompleted(response);
            ExecuteRequestContext context = activeRequests.remove(executionId);
            context.getExecutionCallback().completeOutgoingExecution(executionId);
        }
    }

    /**
     * Handles {@link ProxyOperationControl#operationPrepared(org.jboss.as.controller.NewModelController.OperationTransaction, ModelNode)}
     * method calls done in the remote target controller
     */
    private class OperationPreparedRequestHandler extends ProxyOperationControlRequestHandler {
        volatile int status;
        final CountDownLatch completedLatch = new CountDownLatch(1);


        @Override
        void handle(final int executionId, final ProxyOperationControl control, final ModelNode response)  throws IOException {
            control.operationPrepared(new OperationTransaction() {

                @Override
                public void rollback() {
                    status = NewModelControllerProtocol.PARAM_ROLLBACK;
                    completedLatch.countDown();
                }

                @Override
                public void commit() {
                    status = NewModelControllerProtocol.PARAM_COMMIT;
                    completedLatch.countDown();
                }
            }, response);
        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            output.write(NewModelControllerProtocol.PARAM_PREPARED);
            output.flush();
            try {
                completedLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Thread was interrupted waiting for Tx commit/rollback");
            }
            output.write(status);
        }
    }

    private static class ExecuteRequestContext {
        final OperationMessageHandler messageHandler;
        final ProxyOperationControl control;
        final OperationAttachments attachments;
        final ManagementRequestExecutionCallback executionCallback;

        public ExecuteRequestContext(final OperationMessageHandler messageHandler, final ProxyOperationControl control, final OperationAttachments attachments, final ManagementRequestExecutionCallback executionCallback) {
            this.messageHandler = messageHandler;
            this.control = control;
            this.attachments = attachments;
            this.executionCallback = executionCallback;
        }

        public OperationMessageHandler getMessageHandler() {
            return messageHandler;
        }

        public ProxyOperationControl getControl() {
            return control;
        }

        public OperationAttachments getAttachments() {
            return attachments;
        }

        public ManagementRequestExecutionCallback getExecutionCallback() {
            return executionCallback;
        }
    }
}
