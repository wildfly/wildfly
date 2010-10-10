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
final class ServerManagerCommunicationsActivator implements ServiceActivator, Serializable {
    private final byte[] securityToken;
    private final InetSocketAddress serverManagerAddress;

    private static final ServiceName NAME = ServiceName.JBOSS.append("as.server.manager.communicator");

    private static final long serialVersionUID = 6029538240578468990L;

    ServerManagerCommunicationsActivator(final byte[] securityToken, final InetSocketAddress serverManagerAddress) {
        this.securityToken = securityToken;
        this.serverManagerAddress = serverManagerAddress;
    }

    public void activate(final ServiceActivatorContext context) {
        final BatchBuilder builder = context.getBatchBuilder();
        final ServiceImpl service = new ServiceImpl(securityToken, serverManagerAddress);
        final BatchServiceBuilder<Void> serviceBuilder = builder.addService(NAME, service);
        serviceBuilder.addDependency(ServerController.SERVICE_NAME, ServerController.class, service.getServerControllerInjector());
    }

    static final class ServiceImpl implements Service<Void> {

        private final byte[] token;
        private final InetSocketAddress serverManagerAddress;
        private final InjectedValue<ServerController> serverController = new InjectedValue<ServerController>();

        ServiceImpl(final byte[] token, final InetSocketAddress serverManagerAddress) {
            this.token = token;
            this.serverManagerAddress = serverManagerAddress;
        }

        public void start(final StartContext context) throws StartException {
            // TODO: establish & maintain SM connection
        }

        public void stop(final StopContext context) {
        }

        public Void getValue() throws IllegalStateException {
            return null;
        }

        public Injector<ServerController> getServerControllerInjector() {
            return serverController;
        }
    }
}
