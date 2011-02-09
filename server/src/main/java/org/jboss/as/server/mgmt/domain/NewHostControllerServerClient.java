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

package org.jboss.as.server.mgmt.domain;

import static org.jboss.as.protocol.ProtocolUtils.expectHeader;
import static org.jboss.as.protocol.StreamUtils.safeClose;
import static org.jboss.as.protocol.StreamUtils.writeUTFZBytes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.SimpleByteDataInput;
import org.jboss.as.protocol.SimpleByteDataOutput;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestConnectionStrategy;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ManagementResponse;
import org.jboss.as.protocol.mgmt.ManagementResponseHeader;
import org.jboss.as.server.ServerController;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Client used to interact with the local {@link HostController}.
 *
 * @author John Bailey
 * @author Emanuel Muckenhuber
 */
public class NewHostControllerServerClient implements Service<Void> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "client");
    private final InjectedValue<Connection> smConnection = new InjectedValue<Connection>();
    private final InjectedValue<ServerController> controller = new InjectedValue<ServerController>();
    private final String serverName;

    public NewHostControllerServerClient(final String serverName) {
        this.serverName = serverName;
    }

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        final Connection smConnection = this.smConnection.getValue();
        try {
            new ServerRegisterRequest().executeForResult(new ManagementRequestConnectionStrategy.ExistingConnectionStrategy(smConnection));
        } catch (Exception e) {
            throw new StartException("Failed to send registration message to host controller", e);
        }
        smConnection.setMessageHandler(managementHeaderMessageHandler);
    }

    /** {@inheritDoc} */
    public void stop(StopContext context) {
    }

    /** {@inheritDoc} */
    public Void getValue() throws IllegalStateException {
        return null;
    }

    public Injector<Connection> getSmConnectionInjector() {
        return smConnection;
    }

    public Injector<ServerController> getServerControllerInjector() {
        return controller;
    }

    private MessageHandler managementHeaderMessageHandler = new AbstractMessageHandler() {
        @Override
        public void handle(Connection connection, InputStream dataStream) throws IOException {
            final int workingVersion;
            final ManagementRequestHeader requestHeader;
            ByteDataInput input = null;
            try {
                input = new SimpleByteDataInput(dataStream);

                // Start by reading the request header
                requestHeader = new ManagementRequestHeader(input);

                // Work with the lowest protocol version
                workingVersion = Math.min(ManagementProtocol.VERSION, requestHeader.getVersion());

                byte handlerId = requestHeader.getOperationHandlerId();
                if (handlerId == -1) {
                    throw new IOException("Management request failed.  Invalid handler id");
                }
                connection.setMessageHandler(requestStartHeader);
            } catch (IOException e) {
                throw e;
            } catch (Throwable t) {
                throw new IOException("Failed to read request header", t);
            } finally {
                safeClose(input);
            }

            OutputStream dataOutput = null;
            ByteDataOutput output = null;
            try {
                dataOutput = connection.writeMessage();
                output = new SimpleByteDataOutput(dataOutput);

                // Now write the response header
                final ManagementResponseHeader responseHeader = new ManagementResponseHeader(workingVersion, requestHeader.getRequestId());
                responseHeader.write(output);

                output.close();
                dataOutput.close();
            } catch (IOException e) {
                throw e;
            } catch (Throwable t) {
                throw new IOException("Failed to write management response headers", t);
            } finally {
                safeClose(output);
                safeClose(dataOutput);
            }
        }
    };

    private MessageHandler requestStartHeader = new AbstractMessageHandler() {
        @Override
        public void handle(Connection connection, InputStream input) throws IOException {
            expectHeader(input, ManagementProtocol.REQUEST_OPERATION);
            final byte commandCode = StreamUtils.readByte(input);

            final ManagementResponse operation = operationFor(commandCode);
            if (operation == null) {
                throw new IOException("Invalid command code " + commandCode + " received");
            }
            operation.handle(connection, input);
        }
    };

    private ManagementResponse operationFor(final byte commandByte) {
        switch (commandByte) {
            case NewDomainServerProtocol.EXECUTE_SYNCHRONOUS_REQUEST:
                return new ExecuteSynchronousOperation();
        }
        return null;
    }

    private ModelNode readNode(InputStream in) throws IOException {
        ModelNode node = new ModelNode();
        node.readExternal(in);
        return node;
    }

    private abstract class ExecuteOperation extends ManagementResponse {
        ModelNode operation;

        @Override
        protected final void readRequest(final InputStream inputStream) throws IOException {
            expectHeader(inputStream, NewDomainServerProtocol.PARAM_OPERATION);
            operation = readNode(inputStream);
        }
    }

    private class ExecuteSynchronousOperation extends ExecuteOperation {
        @Override
        protected final byte getResponseCode() {
            return NewDomainServerProtocol.EXECUTE_SYNCHRONOUS_RESPONSE;
        }

        @Override
        protected void sendResponse(final OutputStream outputStream) throws IOException {
            ModelNode result = null;
            try {
                result = controller.getValue().execute(operation);
            } catch (OperationFailedException e) {
                result = new ModelNode().set(createErrorResult(e));
            }
            outputStream.write(NewDomainServerProtocol.PARAM_OPERATION);
            result.writeExternal(outputStream);
        }
    }

    private class ServerRegisterRequest extends ManagementRequest<Void> {
        @Override
        protected byte getHandlerId() {
            return NewDomainServerProtocol.SERVER_TO_HOST_CONTROLLER_OPERATION;
        }

        @Override
        protected byte getRequestCode() {
            return NewDomainServerProtocol.REGISTER_REQUEST;
        }

        @Override
        protected byte getResponseCode() {
            return NewDomainServerProtocol.REGISTER_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            output.write(NewDomainServerProtocol.PARAM_SERVER_NAME);
            writeUTFZBytes(output, serverName);
        }
    }

    static ModelNode createErrorResult(OperationFailedException e) {
        return e.getFailureDescription();
    }

}
