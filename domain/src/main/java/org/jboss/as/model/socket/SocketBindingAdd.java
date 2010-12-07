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

package org.jboss.as.model.socket;

import java.net.InetAddress;

import org.jboss.as.model.AbstractSocketBindingUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.services.net.SocketBindingService;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Emanuel Muckenhuber
 */
public class SocketBindingAdd extends AbstractSocketBindingUpdate {

    private static final long serialVersionUID = -4940876367809265620L;
    private final String name;
    private final String interfaceName;
    private final int port;
    private boolean fixedPort;
    private int multicastPort;
    private InetAddress multicastAddress;

    public SocketBindingAdd(final String interfaceName, final String name, final int port) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (interfaceName == null) {
            throw new IllegalArgumentException("interfaceName is null");
        }
        this.interfaceName = interfaceName;
        this.name = name;
        this.port = port;
    }

    public SocketBindingAdd(SocketBindingElement original) {
        name = original.getName();
        port = original.getPort();
        fixedPort = original.isFixedPort();
        multicastPort = original.getMulticastPort();
        multicastAddress = original.getMulticastAddress();
        interfaceName = original.getInterfaceName();
    }

    /** {@inheritDoc} */
    public AbstractSocketBindingUpdate getCompensatingUpdate(SocketBindingGroupElement original) {
        return new SocketBindingRemove(name);
    }

    /** {@inheritDoc} */
    protected void applyUpdate(SocketBindingGroupElement group) throws UpdateFailedException {
        final SocketBindingElement socketBinding = new SocketBindingElement(name, group.getDefaultInterface());
        socketBinding.setConfiguredInterfaceName(interfaceName);
        socketBinding.setFixedPort(fixedPort);
        socketBinding.setPort(port);
        socketBinding.setMulticastAddress(multicastAddress);
        if(multicastAddress != null) {
            socketBinding.setMulticastPort(multicastPort);
        }
        if(group.addSocketBinding(name, socketBinding)) {
            throw new UpdateFailedException(String.format("duplicate socket-binding (%s) in binding-group (%s)", name, group.getName()));
        }
    }

    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void,P> resultHandler, P param) {
        final ServiceTarget target = updateContext.getServiceTarget().subTarget();
        final UpdateResultHandler.ServiceStartListener<P> listener = new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param);
        target.addListener(listener);
        SocketBindingService.addService(target, this);
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }

    public boolean isFixedPort() {
        return fixedPort;
    }

    public void setFixedPort(boolean fixedPort) {
        this.fixedPort = fixedPort;
    }

    public int getMulticastPort() {
        return multicastPort;
    }

    public void setMulticastPort(int multicastPort) {
        this.multicastPort = multicastPort;
    }

    public InetAddress getMulticastAddress() {
        return multicastAddress;
    }

    public void setMulticastAddress(InetAddress multicastAddress) {
        this.multicastAddress = multicastAddress;
    }

    public String getInterfaceName() {
        return interfaceName;
    }
}
