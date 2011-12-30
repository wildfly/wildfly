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

import static org.jboss.msc.service.ServiceController.Mode.ON_DEMAND;
import static org.jboss.as.host.controller.HostControllerLogger.ROOT_LOGGER;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.AsyncFutureTask;

/**
 * Service providing the {@link ServerInventory}
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class ServerInventoryService implements Service<ServerInventory> {

    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "server-inventory");

    private final InjectedValue<ProcessControllerConnectionService> client = new InjectedValue<ProcessControllerConnectionService>();
    private final InjectedValue<NetworkInterfaceBinding> interfaceBinding = new InjectedValue<NetworkInterfaceBinding>();
    private final DomainController domainController;
    private final HostControllerEnvironment environment;
    private final HostRunningModeControl runningModeControl;
    private final int port;
    private final InjectedValue<ExecutorService> executorService = new InjectedValue<ExecutorService>();

    private final FutureServerInventory futureInventory = new FutureServerInventory();

    private ServerInventoryImpl serverInventory;

    private ServerInventoryService(final DomainController domainController, final HostRunningModeControl runningModeControl, final HostControllerEnvironment environment, final int port) {
        this.domainController = domainController;
        this.runningModeControl = runningModeControl;
        this.environment = environment;
        this.port = port;
    }

    static Future<ServerInventory> install(final ServiceTarget serviceTarget, final DomainController domainController, final HostRunningModeControl runningModeControl, final HostControllerEnvironment environment,
                                           final String interfaceBinding, final int port){
        final ServerInventoryCallbackService callbackService = new ServerInventoryCallbackService();
        serviceTarget.addService(ServerInventoryCallbackService.SERVICE_NAME, callbackService)
                .addDependency(ServerInventoryService.SERVICE_NAME, ServerInventory.class, callbackService.getServerInventoryInjectedValue())
                .setInitialMode(ON_DEMAND)
                .install();

        final ServerInventoryService inventory = new ServerInventoryService(domainController, runningModeControl, environment, port);
        serviceTarget.addService(ServerInventoryService.SERVICE_NAME, inventory)
                .addDependency(HostControllerService.HC_EXECUTOR_SERVICE_NAME, ExecutorService.class, inventory.executorService)
                .addDependency(ProcessControllerConnectionService.SERVICE_NAME, ProcessControllerConnectionService.class, inventory.getClient())
                .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceBinding), NetworkInterfaceBinding.class, inventory.interfaceBinding)
                .install();
        return inventory.futureInventory;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        ROOT_LOGGER.debug("Starting Host Controller Server Inventory");
        try {
            final ProcessControllerConnectionService processControllerConnectionService = client.getValue();
            final InetSocketAddress binding = new InetSocketAddress(interfaceBinding.getValue().getAddress(), port);
            serverInventory = new ServerInventoryImpl(domainController, environment, binding, processControllerConnectionService.getClient());
            processControllerConnectionService.setServerInventory(serverInventory);
            futureInventory.setInventory(serverInventory);
        } catch (Exception e) {
            futureInventory.setFailure(e);
            throw new StartException(e);
        }
    }

    /**
     * Stops all servers.
     *
     * {@inheritDoc}
     */
    @Override
    public synchronized void stop(final StopContext context) {
        if (runningModeControl.getRestartMode() == RestartMode.SERVERS) {
            context.asynchronous();
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    try {
                        serverInventory.stopServers(-1, true); // TODO graceful shutdown // TODO async
                        serverInventory = null;
                        client.getValue().setServerInventory(null);
                    } finally {
                        context.complete();
                    }
                }
            };
            executorService.getValue().execute(r);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized ServerInventory getValue() throws IllegalStateException, IllegalArgumentException {
        final ServerInventory serverInventory = this.serverInventory;
        if(serverInventory == null) {
            throw new IllegalStateException();
        }
        return serverInventory;
    }

    InjectedValue<ProcessControllerConnectionService> getClient() {
        return client;
    }

    private class FutureServerInventory extends AsyncFutureTask<ServerInventory>{

        protected FutureServerInventory() {
            super(null);
        }

        private void setInventory(ServerInventory inventory) {
            super.setResult(inventory);
        }

        private void setFailure(final Throwable t) {
            super.setFailed(t);
        }
    }

}
