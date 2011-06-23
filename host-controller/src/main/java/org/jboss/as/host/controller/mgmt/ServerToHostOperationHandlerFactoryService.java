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

package org.jboss.as.host.controller.mgmt;

import static org.jboss.as.protocol.old.ProtocolUtils.expectHeader;

import java.io.DataInput;
import java.io.IOException;

import org.jboss.as.controller.remote.ManagementOperationHandlerFactory;
import org.jboss.as.host.controller.ManagedServerLifecycleCallback;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.server.mgmt.domain.DomainServerProtocol;
import org.jboss.as.server.mgmt.domain.HostControllerServerClient;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Operation handler responsible for requests coming in from server processes on the host controller.
 * The server side counterpart is {@link HostControllerServerClient}
 *
 * @author John Bailey
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
public class ServerToHostOperationHandlerFactoryService implements ManagementOperationHandlerFactory, Service<ManagementOperationHandlerFactory> {

    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller.mgmt");
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("management", "server", "to", "host", "controller");

    private final InjectedValue<ManagedServerLifecycleCallback> callback = new InjectedValue<ManagedServerLifecycleCallback>();

    private ServerToHostOperationHandlerFactoryService() {
    }

    public static void install(ServiceTarget serviceTarget, ServiceName serverInventoryName) {
        final ServerToHostOperationHandlerFactoryService serverToHost = new ServerToHostOperationHandlerFactoryService();
        serviceTarget.addService(ServerToHostOperationHandlerFactoryService.SERVICE_NAME, serverToHost)
            .addDependency(serverInventoryName, ManagedServerLifecycleCallback.class, serverToHost.callback)
            .install();

    }

    /** {@inheritDoc} */
    @Override
    public void start(StartContext context) throws StartException {
        //
    }

    /** {@inheritDoc} */
    @Override
    public void stop(StopContext context) {
        //
    }

    /** {@inheritDoc} */
    @Override
    public ManagementOperationHandlerFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public ManagementOperationHandler createOperationHandler() {
        return new ServerToHostOperationHandler();
    }

    private class ServerToHostOperationHandler implements ManagementOperationHandler {
        private volatile ManagementOperationHandler proxyOperationHandler;

        public ManagementRequestHandler getRequestHandler(final byte id) {
            if (id == DomainServerProtocol.REGISTER_REQUEST) {
                return new ServerRegisterCommand();
            }
            if (proxyOperationHandler != null) {
                ManagementRequestHandler handler = proxyOperationHandler.getRequestHandler(id);
                if (handler != null) {
                    return handler;
                }
            }
            return null;
        }

        private class ServerRegisterCommand extends ManagementRequestHandler {
            String serverName;
            @Override
            protected void readRequest(final DataInput input) throws IOException {
                expectHeader(input, DomainServerProtocol.PARAM_SERVER_NAME);
                serverName = input.readUTF();
                log.infof("Server [%s] registered using connection [%s]", serverName, getChannel());
            }

            protected void processRequest() {
                ServerToHostOperationHandlerFactoryService.this.callback.getValue().serverRegistered(serverName, getChannel(), new ManagedServerLifecycleCallback.ProxyCreatedCallback() {
                    @Override
                    public void proxyOperationHandlerCreated(ManagementOperationHandler handler) {
                        proxyOperationHandler = handler;
                    }
                });
            }
        }
    }
}
