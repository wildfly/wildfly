/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
