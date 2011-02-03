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

import java.net.InetSocketAddress;

import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Emanuel Muckenhuber
 */
class NewServerInventoryService implements Service<NewServerInventory> {

    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "server-inventory");

    private final InjectedValue<NetworkInterfaceBinding> iFace = new InjectedValue<NetworkInterfaceBinding>();
    private final HostControllerEnvironment environment;
    private final int port;

    private NewServerInventory serverInventory;

    NewServerInventoryService(final HostControllerEnvironment environment, final int port) {
        this.environment = environment;
        this.port = port;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        final NewServerInventory serverInventory;
        try {
            final NetworkInterfaceBinding interfaceBinding = iFace.getValue();
            final InetSocketAddress binding = new InetSocketAddress(interfaceBinding.getAddress(), port);
            serverInventory = new NewServerInventory(environment, binding);
        } catch (Exception e) {
            throw new StartException(e);
        }
        this.serverInventory = serverInventory;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        this.serverInventory = null;
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

    InjectedValue<NetworkInterfaceBinding> getInterface() {
        return iFace;
    }

}
