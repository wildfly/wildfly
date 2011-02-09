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

package org.jboss.as.host.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;

import java.io.IOException;

import org.jboss.as.domain.controller.FileRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service creating the host controller.
 *
 * @author Emanuel Muckenhuber
 */
public class HostControllerService implements Service<HostController> {

    private final InjectedValue<DomainControllerConnection> connection = new InjectedValue<DomainControllerConnection>();
    private final InjectedValue<ServerInventory> serverInventory = new InjectedValue<ServerInventory>();
    private final HostModel hostModel;
    private final FileRepository repository;
    private final String name;

    private HostController controller;

    HostControllerService(final String name, final HostModel hostModel, final FileRepository repository) {
        this.name = name;
        this.hostModel = hostModel;
        this.repository = repository;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        final DomainControllerConnection connection = this.connection.getValue();
        final ServerInventory serverInventory = this.serverInventory.getValue();
        final HostControllerImpl controller = new HostControllerImpl(name, hostModel, serverInventory, repository);
        try {
            final ModelNode domainModel = connection.register(controller);
            final FileRepository remoteRepository = connection.getRemoteFileRepository();
            controller.initDomainConnection(domainModel, remoteRepository);
            // start servers
            final ModelNode rawModel = hostModel.getHostModel();
            if(rawModel.hasDefined(SERVER)) {
                final ModelNode servers = rawModel.get(SERVER).clone();
                for(final String serverName : servers.keys()) {
                    controller.startServer(serverName);
                }
            }
        } catch (IOException e) {
            throw new StartException(e);
        }
        this.controller = controller;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        final HostController controller = this.controller;
        final DomainControllerConnection connection = this.connection.getValue();
        connection.unregister();
        this.controller = null;
        // stop servers
        final ModelNode rawModel = hostModel.getHostModel();
        if(rawModel.hasDefined(SERVER) ) {
            final ModelNode servers = rawModel.get(SERVER).clone();
            for(final String serverName : servers.keys()) {
                controller.stopServer(serverName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized HostController getValue() throws IllegalStateException, IllegalArgumentException {
        final HostController controller = this.controller;
        if(controller == null) {
            throw new IllegalArgumentException();
        }
        return controller;
    }

    InjectedValue<DomainControllerConnection> getConnection() {
        return connection;
    }

    InjectedValue<ServerInventory> getServerInventory() {
        return serverInventory;
    }
}
