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

package org.jboss.as.remoting;

import java.io.IOException;
import java.net.URI;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.xnio.IoFuture;

/**
 * @author Jaikiran Pai
 */
public class OutboundRemotingConnectionService implements Service<OutboundRemotingConnectionService> {

    public static final ServiceName OUTBOUND_CONNECTION_BASE_SERVICE_NAME = RemotingServices.SUBSYSTEM_ENDPOINT.append("outbound-connection");

    private final URI destination;

    private final InjectedValue<Endpoint> endpointInjectedValue = new InjectedValue<Endpoint>();

    public OutboundRemotingConnectionService(final URI destination) {
        if (destination == null) {
            throw new IllegalArgumentException("Destination URI cannot be null while creating a outbound remote connection service");
        }
        this.destination = destination;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public OutboundRemotingConnectionService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    Injector<Endpoint> getEnpointInjector() {
        return this.endpointInjectedValue;
    }

    IoFuture<Connection> connect() throws IOException {
        final Endpoint endpoint = this.endpointInjectedValue.getValue();
        return endpoint.connect(destination);
    }
}
