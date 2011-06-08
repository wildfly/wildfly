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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;

import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.remote.NewRemoteProxyController;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.host.controller.operations.ServerRestartHandler;
import org.jboss.as.host.controller.operations.ServerStartHandler;
import org.jboss.as.host.controller.operations.ServerStatusHandler;
import org.jboss.as.host.controller.operations.ServerStopHandler;
import org.jboss.as.process.ProcessInfo;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * @author Emanuel Muckenhuber
 */
public class HostControllerImpl implements HostController {


    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller");

    private final String name;
    private final ServerInventory serverInventory;
    private final ModelNode model;
    private final ModelNodeRegistration registry;

    private volatile DomainController domainController;

    // FIXME do not leak the model and registry like this!!!!
    HostControllerImpl(final String name, final ModelNode model, final ExtensibleConfigurationPersister configurationPersister,
            final ModelNodeRegistration registry, final ServerInventory serverInventory) {
        this.model = model;
        this.registry = registry;
        this.name = name;
        this.serverInventory = serverInventory;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public ServerStatus getServerStatus(String serverName) {
        final ServerInventory servers = this.serverInventory;
        return servers.determineServerStatus(serverName);
    }

    /** {@inheritDoc} */
    @Override
    public ServerStatus startServer(String serverName) {
        if (domainController == null) {
            throw new IllegalStateException(String.format("Domain Controller is not available; cannot start server %s", serverName));
        }
        final ServerInventory servers = this.serverInventory;
        return servers.startServer(serverName, getHostModel().clone(), domainController);
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
        return servers.restartServer(serverName, gracefulTimeout, getHostModel().clone(), domainController);
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
    public void registerRunningServer(String serverName, ManagementChannel channel) {
        final PathElement element = PathElement.pathElement(RUNNING_SERVER, serverName);

        final NewProxyController serverController = NewRemoteProxyController.create(Executors.newCachedThreadPool(), PathAddress.pathAddress(PathElement.pathElement(HOST, name), element), channel);
        registry.registerProxyController(element, serverController);
        model.get(element.getKey(), element.getValue());
    }

    @Override
    public void unregisterRunningServer(String serverName) {
        PathElement element = PathElement.pathElement(RUNNING_SERVER, serverName);
        model.get(element.getKey()).remove(element.getValue());
        registry.unregisterProxyController(element);
    }

    @Override
    public void startServers(DomainController domainController) {
        this.domainController = domainController;
        // start servers
        final ModelNode rawModel = getHostModel();

        if(rawModel.hasDefined(SERVER_CONFIG)) {
            final ModelNode servers = rawModel.get(SERVER_CONFIG).clone();
            if (serverInventory.getEnvironment().isRestart()){
                restartedHcStartOrReconnectServers(servers);
            } else {
                cleanStartServers(servers);
            }
        }
    }

    private void cleanStartServers(final ModelNode servers){
        for(final String serverName : servers.keys()) {
            if(servers.get(serverName, AUTO_START).asBoolean(true)) {
                try {
                    startServer(serverName);
                } catch (Exception e) {
                    log.errorf(e, "failed to start server (%s)", serverName);
                }
            }
        }
    }

    private void restartedHcStartOrReconnectServers(final ModelNode servers){
        Map<String, ProcessInfo> processInfos = serverInventory.determineRunningProcesses();
        for(final String serverName : servers.keys()) {
            ProcessInfo info = processInfos.get(ManagedServer.getServerProcessName(serverName));
            boolean auto = servers.get(serverName, AUTO_START).asBoolean(true);
            if (info == null && auto) {
                try {
                    startServer(serverName);
                } catch (Exception e) {
                    log.errorf(e, "failed to start server (%s)", serverName);
                }
            } else if (info != null){
                //Reconnect the server
                serverInventory.reconnectServer(serverName, getHostModel(), domainController, info.isRunning());
            }
        }
    }


    @Override
    public void stopServers() {
        final ModelNode rawModel = getHostModel();
        this.domainController = null;
        // stop servers
        if(rawModel.hasDefined(SERVER_CONFIG) ) {
            final ModelNode servers = rawModel.get(SERVER_CONFIG).clone();
            for(final String serverName : servers.keys()) {
                if(servers.get(serverName, AUTO_START).asBoolean(true)) {
                    try {
                        stopServer(serverName);
                    } catch (Exception e) {
                        log.errorf(e, "failed to stop server (%s)", serverName);
                    }
                }
            }
        }
    }

    ModelNode getHostModel() {
        if(domainController == null) {
            throw new IllegalStateException();
        }
        return domainController.getDomainAndHostModel().get(HOST, name);
    }

    protected void registerInternalOperations() {
        ServerStartHandler startHandler = new ServerStartHandler(this);
        ServerRestartHandler restartHandler = new ServerRestartHandler(this);
        ServerStopHandler stopHandler = new ServerStopHandler(this);
        // Register server runtime operation handlers
        ModelNodeRegistration servers = registry.getSubModel(PathAddress.pathAddress(PathElement.pathElement(SERVER_CONFIG)));
        servers.registerMetric(ServerStatusHandler.ATTRIBUTE_NAME, new ServerStatusHandler(this));
        servers.registerOperationHandler(ServerStartHandler.OPERATION_NAME, startHandler, startHandler, false);
        servers.registerOperationHandler(ServerRestartHandler.OPERATION_NAME, restartHandler, restartHandler, false);
        servers.registerOperationHandler(ServerStopHandler.OPERATION_NAME, stopHandler, stopHandler, false);

    }
}
