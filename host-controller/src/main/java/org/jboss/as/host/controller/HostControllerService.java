/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
import org.jboss.as.model.DomainModel;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author John Bailey
 */
public class HostControllerService implements Service<Void> {
    private final HostController hostController;

    private final InjectedValue<NetworkInterfaceBinding> managementInterface = new InjectedValue<NetworkInterfaceBinding>();
    private final InjectedValue<Integer> managementPort = new InjectedValue<Integer>();
    private final InjectedValue<DomainControllerConnection> domainControllerConnection = new InjectedValue<DomainControllerConnection>();

    public HostControllerService(HostController hostController) {
        this.hostController = hostController;
    }

    public void start(StartContext context) throws StartException {
        // Register with the domain controller
        final DomainControllerConnection domainControllerConnection = this.domainControllerConnection.getValue();
        hostController.setDomainControllerConnection(domainControllerConnection);
        final DomainModel domainModel = domainControllerConnection.register();
        hostController.setDomain(domainModel);

        // Start the servers
        final NetworkInterfaceBinding interfaceBinding = managementInterface.getValue();
        final InetSocketAddress managementSocketAddress = new InetSocketAddress(interfaceBinding.getAddress(), managementPort.getValue());
        hostController.setManagementSocketAddress(managementSocketAddress);
        hostController.startServers();
    }

    public void stop(StopContext context) {
    }

    public Void getValue() throws IllegalStateException {
        return null;
    }

    public Injector<NetworkInterfaceBinding> getManagementInterfaceInjector() {
        return managementInterface;
    }

    public Injector<Integer> getManagementPortInjector() {
        return managementPort;
    }

    public Injector<DomainControllerConnection> getDomainControllerConnectionInjector() {
        return domainControllerConnection;
    }
}
