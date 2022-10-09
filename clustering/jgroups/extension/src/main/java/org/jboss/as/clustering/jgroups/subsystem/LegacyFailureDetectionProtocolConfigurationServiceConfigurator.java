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
