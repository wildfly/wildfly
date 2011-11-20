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

import org.jboss.as.network.NetworkMessages;
import org.jboss.as.network.OutboundSocketBinding;

/**
 * Service that represents a remote-destination outbound socket binding
 *
 * @author Jaikiran Pai
 */
public class RemoteDestinationOutboundSocketBindingService extends OutboundSocketBindingService {

    private final String destinationHost;
    private final int destinationPort;

    public RemoteDestinationOutboundSocketBindingService(final String name, final String destinationAddress, final int destinationPort,
                                                         final Integer sourcePort, final boolean fixedSourcePort) {

        super(name, sourcePort, fixedSourcePort);
        if (destinationAddress == null || destinationAddress.trim().isEmpty()) {
            throw NetworkMessages.MESSAGES.nullDestinationAddress(name);
        }
        if (destinationPort < 0) {
            throw NetworkMessages.MESSAGES.negativeDestinationPort(destinationPort, name);
        }
        this.destinationHost = destinationAddress;
        this.destinationPort = destinationPort;
    }

    @Override
    protected OutboundSocketBinding createOutboundSocketBinding() {
        return new OutboundSocketBinding(this.outboundSocketName, this.socketBindingManagerInjectedValue.getValue(),
                destinationHost, destinationPort, this.sourceInterfaceInjectedValue.getOptionalValue(), this.sourcePort,
                this.fixedSourcePort);
    }
}
