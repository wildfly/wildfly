/*
* JBoss, Home of Professional Open Source
* Copyright 2010, Red Hat Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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

import java.net.InetAddress;
import java.util.List;

import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Emanuel Muckenhuber
 */
public class SocketBindingService implements Service<SocketBinding> {

    private final String name;
    private final int port;
    private final boolean isFixedPort;
    private final InetAddress multicastAddress;
    private final int multicastPort;
    private final List<ClientMapping> clientMappings;

    /** The created binding. */
    private SocketBinding binding;

    private final InjectedValue<NetworkInterfaceBinding> interfaceBinding = new InjectedValue<NetworkInterfaceBinding>();
    private final InjectedValue<SocketBindingManager> socketBindings = new InjectedValue<SocketBindingManager>();

    public SocketBindingService(final String name, int port, boolean isFixedPort,
                                InetAddress multicastAddress, int multicastPort,
                                List<ClientMapping> clientMappings) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        this.name = name;
        this.port = port;
        this.isFixedPort = isFixedPort;
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.clientMappings = clientMappings;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        this.binding = new SocketBinding(name, port, isFixedPort,
           multicastAddress, multicastPort,
           interfaceBinding.getOptionalValue(), socketBindings.getValue(), clientMappings);
    }

    @Override
    public synchronized void stop(StopContext context) {
        this.binding = null;
    }

    @Override
    public synchronized SocketBinding getValue() throws IllegalStateException {
        final SocketBinding binding = this.binding;
        if(binding == null) {
            throw new IllegalStateException();
        }
        return binding;
    }

    public InjectedValue<SocketBindingManager> getSocketBindings() {
        return socketBindings;
    }

    public InjectedValue<NetworkInterfaceBinding> getInterfaceBinding() {
        return interfaceBinding;
    }

}
