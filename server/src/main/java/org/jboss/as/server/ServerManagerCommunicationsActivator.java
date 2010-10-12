/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server;

import java.io.Serializable;
import java.net.InetSocketAddress;

import org.jboss.as.server.ServerManagerProtocol.ServerManagerToServerCommandHandler;
import org.jboss.as.server.ServerManagerProtocol.ServerManagerToServerProtocolCommand;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerManagerCommunicationsActivator implements ServiceActivator, Serializable {

    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    private final String processName;
    private final byte[] securityToken;
    private final InetSocketAddress serverManagerAddress;

    private static final ServiceName NAME = ServiceName.JBOSS.append("as.server.manager.communicator");

    private static final long serialVersionUID = 6029538240578468990L;

    public ServerManagerCommunicationsActivator(final String processName, final byte[] securityToken, final InetSocketAddress serverManagerAddress) {
        this.processName = processName;
        this.securityToken = securityToken;
        this.serverManagerAddress = serverManagerAddress;
    }

    public void activate(final ServiceActivatorContext context) {
        final BatchBuilder builder = context.getBatchBuilder();
        final ServiceImpl service = new ServiceImpl(processName, securityToken, serverManagerAddress);
        final BatchServiceBuilder<Void> serviceBuilder = builder.addService(NAME, service);
        // FIXME add this dependency back when we ServerController is doing something
//        serviceBuilder.addDependency(ServerController.SERVICE_NAME, ServerController.class, service.getServerControllerInjector());
    }

    static final class ServiceImpl implements Service<Void> {
        private final String processName;
        private final byte[] token;
        private final InetSocketAddress serverManagerAddress;
        private final InjectedValue<ServerController> serverController = new InjectedValue<ServerController>();
        private ServerCommunicationHandler serverCommunicationHandler;

        ServiceImpl(final String processName, final byte[] token, final InetSocketAddress serverManagerAddress) {
            this.processName = processName;
            this.token = token;
            this.serverManagerAddress = serverManagerAddress;
        }

        public void start(final StartContext context) throws StartException {

            // FIXME this is a temporary communication mechanism
            ServerManagerToServerCommandHandler handler = new ServerManagerToServerCommandHandler() {

                @Override
                public void handleStopServer() {
                    throw new UnsupportedOperationException(ServerManagerToServerProtocolCommand.STOP_SERVER + " is not a valid command");
                }

            };
            this.serverCommunicationHandler = DirectServerSideCommunicationHandler.create(processName, serverManagerAddress.getAddress(), serverManagerAddress.getPort(), handler);
            serverCommunicationHandler.start();
            log.info("Started Server to ServerManager communications");
        }

        public void stop(final StopContext context) {
            if (serverCommunicationHandler != null) {
                serverCommunicationHandler.shutdown();
            }
            log.info("Stopped Server to ServerManager communications");
        }

        public Void getValue() throws IllegalStateException {
            return null;
        }

        public Injector<ServerController> getServerControllerInjector() {
            return serverController;
        }
    }
}
