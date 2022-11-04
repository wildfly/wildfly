/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.jgroups.protocols.FD_SOCK2;

/**
 * @author Paul Ferraro
 */
public class FailureDetectionProtocolConfigurationServiceConfigurator extends SocketProtocolConfigurationServiceConfigurator<FD_SOCK2> {

    public FailureDetectionProtocolConfigurationServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public Map<String, SocketBinding> getSocketBindings() {
        // Socket binding is optional
        SocketBinding binding = this.getSocketBinding();
        return (binding != null) ? Map.of("jgroups.nio.server.fd_sock", binding) : Map.of();
    }

    @Override
    public void accept(FD_SOCK2 protocol) {
        SocketBinding protocolBinding = this.getSocketBinding();
        SocketBinding transportBinding = this.getTransport().getSocketBinding();

        InetSocketAddress protocolBindAddress = (protocolBinding != null) ? protocolBinding.getSocketAddress() : null;
        InetSocketAddress transportBindAddress = transportBinding.getSocketAddress();
        protocol.setBindAddress(((protocolBindAddress != null) ? protocolBindAddress : transportBindAddress).getAddress());

        if (protocolBinding != null) {
            protocol.setOffset(protocolBindAddress.getPort() - transportBindAddress.getPort());

            List<ClientMapping> clientMappings = protocolBinding.getClientMappings();
            if (!clientMappings.isEmpty()) {
                // JGroups cannot select a client mapping based on the source address, so just use the first one
                ClientMapping mapping = clientMappings.get(0);
                try {
                    protocol.setExternalAddress(InetAddress.getByName(mapping.getDestinationAddress()));
                    protocol.setExternalPort(mapping.getDestinationPort());
                } catch (UnknownHostException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }

        SocketBinding clientBinding = this.getClientSocketBinding();
        if (clientBinding != null) {
            protocol.setClientBindPort(clientBinding.getSocketAddress().getPort());
        }
    }
}
