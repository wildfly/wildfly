/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.SocketBinding;
import org.jgroups.protocols.UDP;

/**
 * Custom builder for transports that need to configure a multicast socket.
 * @author Paul Ferraro
 */
public class MulticastTransportConfigurationServiceConfigurator extends TransportConfigurationServiceConfigurator<UDP> {

    public MulticastTransportConfigurationServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public void accept(UDP protocol) {
        SocketBinding binding = this.getSocketBinding();
        protocol.setMulticasting(binding.getMulticastAddress() != null);
        if (protocol.supportsMulticasting()) {
            protocol.setMulticastAddress(binding.getMulticastAddress());
            protocol.setMulticastPort(binding.getMulticastPort());
        }
        super.accept(protocol);
    }
}
