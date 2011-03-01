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

import java.util.concurrent.CancellationException;

import org.jboss.as.controller.ControllerTransactionContext;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ExecutionContext;
import org.jboss.as.domain.controller.DomainControllerSlave;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.ServerStartupTransactionalProxyController;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
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
public class HostControllerService implements Service<ServerStartupTransactionalProxyController> {

    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller");
    private final InjectedValue<ServerInventory> serverInventory = new InjectedValue<ServerInventory>();
    private final HostModel hostModel;
    private final FileRepository repository;
    private final String name;

    private HostController controller;
    private ServerStartupTransactionalProxyController proxyController;

    HostControllerService(final String name, final HostModel hostModel, final FileRepository repository) {
        this.name = name;
        this.hostModel = hostModel;
        this.repository = repository;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        final ServerInventory serverInventory = this.serverInventory.getValue();
        final HostControllerImpl controller = new HostControllerImpl(name, hostModel, serverInventory, repository);
        serverInventory.setHostController(controller);
        this.controller = controller;
        this.proxyController = new ServerStartupTransactionalProxyController() {

            @Override
            public OperationResult execute(ExecutionContext executionContext, ResultHandler handler, ControllerTransactionContext transaction) {
                return controller.execute(executionContext, handler, transaction);
            }

            @Override
            public ModelNode execute(ExecutionContext executionContext) throws CancellationException {
                return controller.execute(executionContext);
            }

            @Override
            public OperationResult execute(ExecutionContext executionContext, ResultHandler handler) {
                return controller.execute(executionContext, handler);
            }

            @Override
            public PathAddress getProxyNodeAddress() {
                return PathAddress.pathAddress(PathElement.pathElement("host", name));
            }

            public void startServers(DomainControllerSlave domainController) {
                controller.startServers(domainController);
            }

            public void stopServers() {
                controller.stopServers();
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        this.controller = null;
        this.proxyController = null;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized ServerStartupTransactionalProxyController getValue() throws IllegalStateException, IllegalArgumentException {
        final ServerStartupTransactionalProxyController controller = this.proxyController;
        if(controller == null) {
            throw new IllegalArgumentException();
        }
        return controller;
    }

    InjectedValue<ServerInventory> getServerInventory() {
        return serverInventory;
    }
}
