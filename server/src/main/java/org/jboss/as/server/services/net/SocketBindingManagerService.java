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

import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.as.network.SocketBindingManagerImpl;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Emanuel Muckenhuber
 */
public class SocketBindingManagerService extends SocketBindingManagerImpl implements Service<SocketBindingManager> {

    private final InjectedValue<NetworkInterfaceBinding> defaultInterfaceBinding = new InjectedValue<NetworkInterfaceBinding>();
    private final int portOffSet;

    public SocketBindingManagerService(int portOffSet) {
        this.portOffSet = portOffSet;
    }

    @Override
    public void start(StartContext context) throws StartException {
        //
    }

    @Override
    public void stop(StopContext context) {

    }

    @Override
    public SocketBindingManager getValue() throws IllegalStateException {
        return this;
    }

    @Override
    public int getPortOffset() {
        return portOffSet;
    }

    @Override
    public InetAddress getDefaultInterfaceAddress() {
        return defaultInterfaceBinding.getValue().getAddress();
    }

    @Override
    public NetworkInterfaceBinding getDefaultInterfaceBinding() {
        return defaultInterfaceBinding.getValue();
    }

    public InjectedValue<NetworkInterfaceBinding> getDefaultInterfaceBindingInjector() {
        return defaultInterfaceBinding;
    }

}

