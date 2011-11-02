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

import java.net.InetAddress;

/**
 * Service that represents a remote-destination outbound socket binding
 *
 * @author Jaikiran Pai
 */
public class RemoteDestinationOutboundSocketBindingService extends OutboundSocketBindingService {

    private final InetAddress destinationHost;
    private final int destinationPort;

    public RemoteDestinationOutboundSocketBindingService(final String name, final InetAddress destinationHost, final int destinationPort,
                                                         final Integer sourcePort, final boolean fixedSourcePort) {

        super(name, sourcePort, fixedSourcePort);
        if (destinationHost == null) {
            throw new IllegalArgumentException("Destination host cannot be null for outbound socket binding " + name);
        }
        if (destinationPort < 0) {
            throw new IllegalArgumentException("Destination port cannot be a negative value for outbound socket binding " + name);
        }
        this.destinationHost = destinationHost;
        this.destinationPort = destinationPort;
    }

    @Override
    protected InetAddress getDestinationAddress() {
        return this.destinationHost;
    }

    @Override
    protected int getDestinationPort() {
        return this.destinationPort;
    }
}
