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
package org.jboss.as.host.controller.mgmt;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.protocol.ProtocolUtils.expectHeader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CancellationException;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.ModelControllerClient.Type;
import org.jboss.as.controller.remote.ModelControllerOperationHandlerImpl;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.HostControllerClient;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.ManagementResponse;
import org.jboss.dmr.ModelNode;

/**
 * Standard ModelController operation handler that also has the operations for HC->DC.
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class DomainControllerOperationHandlerImpl extends ModelControllerOperationHandlerImpl {

    public DomainControllerOperationHandlerImpl(Type type, DomainController modelController, MessageHandler initiatingHandler) {
        super(type, modelController, initiatingHandler);
    }

    @Override
    protected DomainController getController() {
        return (DomainController)super.getController();
    }

    @Override
    public ManagementResponse operationFor(byte commandByte) {
        switch (commandByte) {
        case DomainControllerProtocol.REGISTER_HOST_CONTROLLER_REQUEST:
            return new RegisterOperation();
        case DomainControllerProtocol.UNREGISTER_HOST_CONTROLLER_REQUEST:
            return new UnregisterOperation();
        default:
            return super.operationFor(commandByte);
        }
    }

    private abstract class RegistryOperation extends ManagementResponse {
        String hostId;

        RegistryOperation() {
            super(getInitiatingHandler());
        }

        @Override
        protected final void readRequest(final InputStream inputStream) throws IOException {
            expectHeader(inputStream, DomainControllerProtocol.PARAM_HOST_ID);
            hostId = StreamUtils.readUTFZBytes(inputStream);
        }
    }

    private class RegisterOperation extends RegistryOperation {
        Connection connection;
        @Override
        protected final byte getResponseCode() {
            return DomainControllerProtocol.REGISTER_HOST_CONTROLLER_RESPONSE;
        }

        @Override
        public void handle(final Connection connection, final InputStream input) throws IOException {
            this.connection = connection;
            super.handle(connection, input);
        }


        @Override
        protected void sendResponse(final OutputStream outputStream) throws IOException {
            getController().addClient(new RemoteHostControllerClient(hostId, connection));
            ModelNode node = getController().getDomainModel();
            outputStream.write(DomainControllerProtocol.PARAM_MODEL);
            node.writeExternal(outputStream);
        }
    }

    private class UnregisterOperation extends RegistryOperation {
        @Override
        protected final byte getResponseCode() {
            return DomainControllerProtocol.UNREGISTER_HOST_CONTROLLER_RESPONSE;
        }

        @Override
        protected void sendResponse(final OutputStream outputStream) throws IOException {
            getController().removeClient(hostId);
        }
    }


    private class RemoteHostControllerClient implements HostControllerClient {

        final ProxyController remote;
        final Connection connection;
        final String hostId;
        final PathAddress proxyNodeAddress;

        public RemoteHostControllerClient(String hostId, Connection connection) {
            this.hostId = hostId;
            this.connection = connection;
            this.proxyNodeAddress = PathAddress.pathAddress(PathElement.pathElement(HOST, getId()));
            this.remote = RemoteProxyController.create(ModelControllerClient.Type.HOST, connection, proxyNodeAddress);
        }

        @Override
        public PathAddress getProxyNodeAddress() {
            return remote.getProxyNodeAddress();
        }

        @Override
        public Cancellable execute(ModelNode operation, ResultHandler handler) {
            System.out.println("===== RHCC execute()");
            return remote.execute(operation, handler);
        }

        @Override
        public ModelNode execute(ModelNode operation) throws CancellationException, OperationFailedException {
            return remote.execute(operation);
        }

        @Override
        public String getId() {
            return hostId;
        }

        @Override
        public boolean isActive() {
            return connection.getPeerAddress() != null;
        }

    }
}
