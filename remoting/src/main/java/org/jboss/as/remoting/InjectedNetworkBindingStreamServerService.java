/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.remoting;

import java.net.InetSocketAddress;

import org.jboss.as.network.ManagedBinding;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.msc.value.InjectedValue;
import org.xnio.OptionMap;

/**
 * {@link AbstractStreamServerService} that uses an injected network interface binding service.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class InjectedNetworkBindingStreamServerService extends AbstractStreamServerService {

    private final InjectedValue<NetworkInterfaceBinding> interfaceBindingValue = new InjectedValue<NetworkInterfaceBinding>();
    private final int port;

    public InjectedNetworkBindingStreamServerService(final OptionMap connectorPropertiesOptionMap, int port) {
        super(connectorPropertiesOptionMap);
        this.port = port;
    }

    public InjectedValue<NetworkInterfaceBinding> getInterfaceBindingInjector(){
        return interfaceBindingValue;
    }

    @Override
    InetSocketAddress getSocketAddress() {
        return new InetSocketAddress(interfaceBindingValue.getValue().getAddress(), port);
    }

    @Override
    ManagedBinding registerSocketBinding(SocketBindingManager socketBindingManager) {
        InetSocketAddress address = new InetSocketAddress(interfaceBindingValue.getValue().getAddress(), port);
        ManagedBinding binding = ManagedBinding.Factory.createSimpleManagedBinding("management-native", address, null);
        socketBindingManager.getUnnamedRegistry().registerBinding(binding);
        return binding;
    }

    @Override
    void unregisterSocketBinding(ManagedBinding managedBinding, SocketBindingManager socketBindingManager) {
        socketBindingManager.getUnnamedRegistry().unregisterBinding(managedBinding);
    }
}
