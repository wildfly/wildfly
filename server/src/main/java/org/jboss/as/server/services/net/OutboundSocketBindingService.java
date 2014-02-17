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

import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service that represents an outbound socket binding
 *
 * @author Jaikiran Pai
 */
public abstract class OutboundSocketBindingService implements Service<OutboundSocketBinding> {

    protected final String outboundSocketName;
    protected final Integer sourcePort;
    protected final InjectedValue<SocketBindingManager> socketBindingManagerInjectedValue = new InjectedValue<SocketBindingManager>();
    protected final InjectedValue<NetworkInterfaceBinding> sourceInterfaceInjectedValue = new InjectedValue<NetworkInterfaceBinding>();
    protected final boolean fixedSourcePort;

    private volatile OutboundSocketBinding outboundSocketBinding;

    public OutboundSocketBindingService(final String name, final Integer sourcePort, final boolean fixedSourcePort) {
        this.outboundSocketName = name;
        this.sourcePort = sourcePort;
        this.fixedSourcePort = fixedSourcePort;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        this.outboundSocketBinding = this.createOutboundSocketBinding();
    }

    @Override
    public synchronized void stop(StopContext context) {
        this.outboundSocketBinding = null;
    }

    @Override
    public synchronized OutboundSocketBinding getValue() throws IllegalStateException, IllegalArgumentException {
        return this.outboundSocketBinding;
    }

    protected Injector<SocketBindingManager> getSocketBindingManagerInjector() {
        return this.socketBindingManagerInjectedValue;
    }

    protected Injector<NetworkInterfaceBinding> getSourceNetworkInterfaceBindingInjector() {
        return this.sourceInterfaceInjectedValue;
    }

    /**
     * Create and return the {@link OutboundSocketBinding} for this outbound socket binding service
     * @return
     */
    protected abstract OutboundSocketBinding createOutboundSocketBinding();
}
