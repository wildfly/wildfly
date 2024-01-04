/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.jgroups.protocols.FD_SOCK;

/**
 * @author Paul Ferraro
 */
public class LegacyFailureDetectionProtocolConfigurationServiceConfigurator extends SocketProtocolConfigurationServiceConfigurator<FD_SOCK> {

    public LegacyFailureDetectionProtocolConfigurationServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public Map<String, SocketBinding> getSocketBindings() {
        Map<String, SocketBinding> result = new TreeMap<>();
        SocketBinding binding = this.getSocketBinding();
        if (binding != null) {
            result.put("jgroups.fd_sock.srv_sock", binding);
        }
        SocketBinding clientBinding = this.getClientSocketBinding();
        if (clientBinding != null) {
            result.put("jgroups.fd.ping_sock", clientBinding);
        }
        return result;
    }

    @Override
    public void accept(FD_SOCK protocol) {
        // If binding is undefined, protocol will use bind address of transport and a random ephemeral port
        SocketBinding binding = this.getSocketBinding();
        protocol.setStartPort((binding != null) ? binding.getAbsolutePort() : 0);

        if (binding != null) {
            protocol.setBindAddress(binding.getAddress());

            List<ClientMapping> clientMappings = binding.getClientMappings();
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
