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

package org.jboss.as.server.services.net;

import org.jboss.as.network.ClientSocketBinding;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.net.InetAddress;

/**
 * @author Jaikiran Pai
 */
public abstract class ClientSocketBindingService implements Service<ClientSocketBinding> {

    protected final String clientSocketName;
    private final Integer sourcePort;
    private final InjectedValue<SocketBindingManager> socketBindingManagerInjectedValue = new InjectedValue<SocketBindingManager>();
    private final InjectedValue<NetworkInterfaceBinding> sourceInterfaceInjectedValue = new InjectedValue<NetworkInterfaceBinding>();
    private final boolean fixedSourcePort;

    private volatile ClientSocketBinding clientSocketBinding;

    public ClientSocketBindingService(final String name, final Integer sourcePort, final boolean fixedSourcePort) {
        this.clientSocketName = name;
        this.sourcePort = sourcePort;
        this.fixedSourcePort = fixedSourcePort;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final InetAddress destinationHost = this.getDestinationAddress();
        if (destinationHost == null) {
            throw new IllegalStateException("Destination host cannot be null for client socket binding: " + this.clientSocketName);
        }
        final int destinationPort = this.getDestinationPort();
        if (destinationPort < 0) {
            throw new IllegalStateException("Destination port " + destinationPort + " cannot be negative for client socket binding: " + this.clientSocketName);
        }
        this.clientSocketBinding = new ClientSocketBinding(this.clientSocketName, this.socketBindingManagerInjectedValue.getValue(),
                destinationHost, destinationPort, this.sourceInterfaceInjectedValue.getOptionalValue(), this.sourcePort, this.fixedSourcePort);
    }

    @Override
    public synchronized void stop(StopContext context) {
        this.clientSocketBinding = null;
    }

    @Override
    public synchronized ClientSocketBinding getValue() throws IllegalStateException, IllegalArgumentException {
        return this.clientSocketBinding;
    }

    protected Injector<SocketBindingManager> getSocketBindingManagerInjector() {
        return this.socketBindingManagerInjectedValue;
    }

    protected Injector<NetworkInterfaceBinding> getSourceNetworkInterfaceBindingInjector() {
        return this.sourceInterfaceInjectedValue;
    }

    protected abstract InetAddress getDestinationAddress();

    protected abstract int getDestinationPort();

}
