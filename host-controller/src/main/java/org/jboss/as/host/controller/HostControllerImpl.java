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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;

import java.util.concurrent.CancellationException;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.domain.client.api.ServerStatus;
import org.jboss.as.domain.controller.DomainModel;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.protocol.Connection;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * @author Emanuel Muckenhuber
 */
public class HostControllerImpl implements HostController {


    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller");
    private static final ExtensibleConfigurationPersister domainPersister = new NullConfigurationPersister(null);

    private final String name;
    private final HostModel hostModel;
    private final ServerInventory serverInventory;
    private final FileRepository repository;

    private DomainModel domainModel;
    private FileRepository remoteRepository;

    HostControllerImpl(final String name, final HostModel model, final ServerInventory serverInventory, final FileRepository repository) {
        this.name = name;
        this.hostModel = model;
        this.repository = repository;
        this.serverInventory = serverInventory;
    }

    void initDomainConnection(final ModelNode domainModel, final FileRepository remoteRepository) {
        assert domainModel != null : "null domain model";
        /// assert remoteRepository != null : "null remote repository";
        this.domainModel = DomainModel.Factory.create(domainModel, domainPersister);
        this.remoteRepository = remoteRepository;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public Cancellable execute(ModelNode operation, ResultHandler handler) {

        return hostModel.execute(operation, handler);
    }

    /** {@inheritDoc} */
    @Override
    public ModelNode execute(ModelNode operation) throws CancellationException, OperationFailedException {

        return hostModel.execute(operation);
    }

    /** {@inheritDoc} */
    @Override
    public ServerStatus startServer(String serverName) {
        final ServerInventory servers = this.serverInventory;
        return servers.startServer(serverName, hostModel.getHostModel(), domainModel.getDomainModel());
    }

    /** {@inheritDoc} */
    @Override
    public ServerStatus restartServer(String serverName) {
        return restartServer(serverName, -1);
    }

    /** {@inheritDoc} */
    @Override
    public ServerStatus restartServer(String serverName, int gracefulTimeout) {
        final ServerInventory servers = this.serverInventory;
        return servers.restartServer(serverName, gracefulTimeout, hostModel.getHostModel(), domainModel.getDomainModel());
    }

    /** {@inheritDoc} */
    @Override
    public ServerStatus stopServer(String serverName) {
        return stopServer(serverName, -1);
    }

    /** {@inheritDoc} */
    @Override
    public ServerStatus stopServer(String serverName, int gracefulTimeout) {
        final ServerInventory servers = this.serverInventory;
        return servers.stopServer(serverName, gracefulTimeout);
    }

    @Override
    public void registerRunningServer(String serverName, Connection connection) {
        PathElement element = PathElement.pathElement(RUNNING_SERVER, serverName);
        ProxyController serverController = RemoteProxyController.create(ModelControllerClient.Type.STANDALONE, connection, PathAddress.pathAddress(element));
        hostModel.registerProxy(serverController);
    }

    @Override
    public void unregisterRunningServer(String serverName) {
        hostModel.unregisterProxy(serverName);
    }

}
