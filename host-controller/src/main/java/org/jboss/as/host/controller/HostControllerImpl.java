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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;

import java.util.concurrent.CancellationException;

import org.jboss.as.controller.ControllerTransactionContext;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ExecutionContext;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.domain.client.api.ServerStatus;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.protocol.Connection;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * @author Emanuel Muckenhuber
 */
public class HostControllerImpl implements HostController {


    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller");

    private final String name;
    private final HostModel hostModel;
    private final ServerInventory serverInventory;

    private volatile DomainController domainController;

    HostControllerImpl(final String name, final HostModel model, final ServerInventory serverInventory) {
        this.name = name;
        this.hostModel = model;
        this.serverInventory = serverInventory;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(ExecutionContext executionContext, ResultHandler handler, ControllerTransactionContext transaction) {
        return hostModel.execute(executionContext, handler, transaction);
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(ExecutionContext executionContext, ResultHandler handler) {
        return hostModel.execute(executionContext, handler);
    }

    /** {@inheritDoc} */
    @Override
    public ModelNode execute(ExecutionContext executionContext) throws CancellationException {

        return hostModel.execute(executionContext);
    }

    /** {@inheritDoc} */
    @Override
    public ModelNode execute(ExecutionContext executionContext, ControllerTransactionContext transaction) throws CancellationException {

        return hostModel.execute(executionContext, transaction);
    }

    /** {@inheritDoc} */
    @Override
    public ServerStatus startServer(String serverName) {
        if (domainController == null) {
            throw new IllegalStateException(String.format("Domain Controller is not available; cannot start server %s", serverName));
        }
        final ServerInventory servers = this.serverInventory;
        return servers.startServer(serverName, hostModel.getHostModel(), domainController);
    }

    /** {@inheritDoc} */
    @Override
    public ServerStatus restartServer(String serverName) {
        return restartServer(serverName, -1);
    }

    /** {@inheritDoc} */
    @Override
    public ServerStatus restartServer(String serverName, int gracefulTimeout) {
        if (domainController == null) {
            throw new IllegalStateException(String.format("Domain Controller is not available; cannot restart server %s", serverName));
        }
        final ServerInventory servers = this.serverInventory;
        return servers.restartServer(serverName, gracefulTimeout, hostModel.getHostModel(), domainController);
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
        ProxyController serverController = RemoteProxyController.create(connection, PathAddress.pathAddress(element));
        hostModel.registerProxy(serverController);
    }

    @Override
    public void unregisterRunningServer(String serverName) {
        hostModel.unregisterProxy(serverName);
    }

    @Override
    public void startServers(DomainController domainController) {
        this.domainController = domainController;

        // start servers
        final ModelNode rawModel = hostModel.getHostModel();
        if(rawModel.hasDefined(SERVER_CONFIG)) {
            final ModelNode servers = rawModel.get(SERVER_CONFIG).clone();
            for(final String serverName : servers.keys()) {
                if(servers.get(serverName, START).asBoolean(true)) {
                    try {
                        startServer(serverName);
                    } catch (Exception e) {
                        log.errorf(e, "failed to start server (%s)", serverName);
                    }
                }
            }
        }
    }

    @Override
    public void stopServers() {
        this.domainController = null;
        // stop servers
        final ModelNode rawModel = hostModel.getHostModel();
        if(rawModel.hasDefined(SERVER_CONFIG) ) {
            final ModelNode servers = rawModel.get(SERVER_CONFIG).clone();
            for(final String serverName : servers.keys()) {
                if(servers.get(serverName, START).asBoolean(true)) {
                    try {
                        stopServer(serverName);
                    } catch (Exception e) {
                        log.errorf(e, "failed to stop server (%s)", serverName);
                    }
                }
            }
        }
    }
}
