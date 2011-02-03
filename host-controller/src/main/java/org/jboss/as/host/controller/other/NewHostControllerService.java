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

package org.jboss.as.host.controller.other;

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
public class NewHostControllerService implements Service<NewHostController> {

    private final InjectedValue<NewDomainControllerConnection> connection = new InjectedValue<NewDomainControllerConnection>();
    private final InjectedValue<NewServerInventory> serverInventory = new InjectedValue<NewServerInventory>();
    private final NewHostModel hostModel;
    private final FileRepository repository;
    private final String name;

    private NewHostController controller;

    NewHostControllerService(final String name, final NewHostModel hostModel, final FileRepository repository) {
        this.name = name;
        this.hostModel = hostModel;
        this.repository = repository;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        final NewDomainControllerConnection connection = this.connection.getValue();
        final NewServerInventory serverInventory = this.serverInventory.getValue();
        final NewHostControllerImpl controller = new NewHostControllerImpl(name, hostModel, serverInventory, repository);
        try {
            final ModelNode domainModel = connection.register(controller);
            final FileRepository remoteRepository = connection.getRemoteFileRepository();
            controller.initDomainConnection(domainModel, remoteRepository);
            // start servers
            if(hostModel.getModel().has(SERVER) && hostModel.getModel().get(SERVER).isDefined()) {
                final ModelNode servers = hostModel.getModel().get(SERVER).clone();
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
        final NewHostController controller = this.controller;
        final NewDomainControllerConnection connection = this.connection.getValue();
        connection.unregister();
        this.controller = null;
        // stop servers
        if(hostModel.getModel().has(SERVER) && hostModel.getModel().get(SERVER).isDefined()) {
            final ModelNode servers = hostModel.getModel().get(SERVER).clone();
            for(final String serverName : servers.keys()) {
                controller.stopServer(serverName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized NewHostController getValue() throws IllegalStateException, IllegalArgumentException {
        final NewHostController controller = this.controller;
        if(controller == null) {
            throw new IllegalArgumentException();
        }
        return controller;
    }

    InjectedValue<NewDomainControllerConnection> getConnection() {
        return connection;
    }

    InjectedValue<NewServerInventory> getServerInventory() {
        return serverInventory;
    }
}
