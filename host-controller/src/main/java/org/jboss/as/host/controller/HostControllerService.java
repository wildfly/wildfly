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

import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.LocalHostModel;
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
public class HostControllerService implements Service<LocalHostModel> {

    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller");
    private final InjectedValue<ServerInventory> serverInventory = new InjectedValue<ServerInventory>();
    private final ModelNode hostModel;
    private final ExtensibleConfigurationPersister configPersister;
    private final ModelNodeRegistration registry;
    private final String name;

    private LocalHostModel proxyController;

    HostControllerService(final String name, final ModelNode hostModel,
            final ExtensibleConfigurationPersister configPersister, final ModelNodeRegistration registry) {
        this.name = name;
        this.hostModel = hostModel;
        this.configPersister = configPersister;
        this.registry = registry;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        final ServerInventory serverInventory = this.serverInventory.getValue();
        final HostControllerImpl controller = new HostControllerImpl(name, hostModel, configPersister, registry, serverInventory);
        serverInventory.setHostController(controller);
        controller.registerInternalOperations();
        this.proxyController = new LocalHostModel() {

            @Override
            public void startServers(DomainController domainController) {
                controller.startServers(domainController);
            }

            @Override
            public void stopServers() {
                controller.stopServers();
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public ModelNode getHostModel() {
                return hostModel;
            }

            @Override
            public ModelNodeRegistration getRegistry() {
                return registry;
            }

            @Override
            public ExtensibleConfigurationPersister getConfigurationPersister() {
                return configPersister;
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        this.proxyController = null;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized LocalHostModel getValue() throws IllegalStateException, IllegalArgumentException {
        final LocalHostModel controller = this.proxyController;
        if(controller == null) {
            throw new IllegalArgumentException();
        }
        return controller;
    }

    InjectedValue<ServerInventory> getServerInventory() {
        return serverInventory;
    }
}
