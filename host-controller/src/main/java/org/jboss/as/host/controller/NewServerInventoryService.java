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

import java.net.InetSocketAddress;
import java.util.concurrent.Future;

import org.jboss.as.domain.controller.NewDomainController;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.process.ProcessControllerClient;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.AsyncFutureTask;

/**
 * @author Emanuel Muckenhuber
 */
class NewServerInventoryService implements Service<NewServerInventory> {
    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller");

    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "server-inventory");

    private final InjectedValue<NewProcessControllerConnectionService> client = new InjectedValue<NewProcessControllerConnectionService>();
    private final NetworkInterfaceBinding interfaceBinding;
    private final NewDomainController domainController;
    private final HostControllerEnvironment environment;
    private final int port;
    private final FutureServerInventory futureInventory = new FutureServerInventory();

    private NewServerInventory serverInventory;

    private NewServerInventoryService(final NewDomainController domainController, final HostControllerEnvironment environment, final NetworkInterfaceBinding interfaceBinding, final int port) {
        this.domainController = domainController;
        this.environment = environment;
        this.interfaceBinding = interfaceBinding;
        this.port = port;
    }

    static Future<NewServerInventory> install(final ServiceTarget serviceTarget, final NewDomainController domainController, final HostControllerEnvironment environment, final NetworkInterfaceBinding interfaceBinding, final int port){
        final NewServerInventoryService inventory = new NewServerInventoryService(domainController, environment, interfaceBinding, port);
        serviceTarget.addService(NewServerInventoryService.SERVICE_NAME, inventory)
                .addDependency(NewProcessControllerConnectionService.SERVICE_NAME, NewProcessControllerConnectionService.class, inventory.getClient())
                .install();
        return inventory.futureInventory;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        log.debug("Starting Host Controller Server Inventory");
        final NewServerInventory serverInventory;
        try {
            final ProcessControllerClient client = this.client.getValue().getClient();
            final InetSocketAddress binding = new InetSocketAddress(interfaceBinding.getAddress(), port);
            serverInventory = new NewServerInventoryImpl(domainController, environment, binding, client);
        } catch (Exception e) {
            throw new StartException(e);
        }
        this.serverInventory = serverInventory;
        client.getValue().setServerInventory(serverInventory);
        futureInventory.setInventory(serverInventory);
    }

    public Future<NewServerInventory> getInventoryFuture(){
        return futureInventory;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        this.serverInventory.stopServers(-1); // TODO graceful shutdown // TODO async
        this.serverInventory = null;
        client.getValue().setServerInventory(null);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized NewServerInventory getValue() throws IllegalStateException, IllegalArgumentException {
        final NewServerInventory serverInventory = this.serverInventory;
        if(serverInventory == null) {
            throw new IllegalStateException();
        }
        return serverInventory;
    }

    InjectedValue<NewProcessControllerConnectionService> getClient() {
        return client;
    }

    private class FutureServerInventory extends AsyncFutureTask<NewServerInventory>{

        protected FutureServerInventory() {
            super(null);
        }

        private void setInventory(NewServerInventory inventory) {
            super.setResult(inventory);
        }
    }

}
