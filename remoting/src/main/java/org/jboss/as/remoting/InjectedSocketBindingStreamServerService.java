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
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.msc.value.InjectedValue;
import org.xnio.OptionMap;

/**
 * {@link AbstractStreamServerService} that uses an injected socket binding.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class InjectedSocketBindingStreamServerService extends AbstractStreamServerService {

    private final InjectedValue<SocketBinding> socketBindingValue = new InjectedValue<SocketBinding>();

    public InjectedSocketBindingStreamServerService(final OptionMap connectorPropertiesOptionMap) {
        super(connectorPropertiesOptionMap);
    }

    public InjectedValue<SocketBinding> getSocketBindingInjector(){
        return socketBindingValue;
    }

    @Override
    InetSocketAddress getSocketAddress() {
        return socketBindingValue.getValue().getSocketAddress();
    }

    @Override
    ManagedBinding registerSocketBinding(SocketBindingManager socketBindingManager) {
        ManagedBinding binding = ManagedBinding.Factory.createSimpleManagedBinding(socketBindingValue.getValue());
        socketBindingManager.getNamedRegistry().registerBinding(binding);
        return binding;
    }

    @Override
    void unregisterSocketBinding(ManagedBinding managedBinding, SocketBindingManager socketBindingManager) {
        socketBindingManager.getNamedRegistry().unregisterBinding(managedBinding);
    }

    /**
     * Returns the socket binding applicable for this {@link InjectedSocketBindingStreamServerService}
     * @return
     */
    public SocketBinding getSocketBinding() {
        return this.socketBindingValue.getValue();
    }

}
