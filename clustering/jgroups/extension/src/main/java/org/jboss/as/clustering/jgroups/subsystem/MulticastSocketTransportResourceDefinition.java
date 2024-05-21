/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.UDP;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;

/**
 * Resource definition for transports that need to configure a multicast socket.
 * @author Paul Ferraro
 */
public class MulticastSocketTransportResourceDefinition extends TransportResourceDefinition<UDP> {

    MulticastSocketTransportResourceDefinition(String name) {
        super(name);
    }

    @Override
    public Map.Entry<Function<ProtocolConfiguration<UDP>, TransportConfiguration<UDP>>, Consumer<RequirementServiceBuilder<?>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        Map.Entry<Function<ProtocolConfiguration<UDP>, TransportConfiguration<UDP>>, Consumer<RequirementServiceBuilder<?>>> entry = super.resolve(context, model);
        return Map.entry(entry.getKey().andThen(new UnaryOperator<>() {
            @Override
            public TransportConfiguration<UDP> apply(TransportConfiguration<UDP> configuration) {
                return new TransportConfigurationDecorator<>(configuration) {
                    @Override
                    public UDP createProtocol(ProtocolStackConfiguration stackConfiguration) {
                        SocketBinding binding = this.getSocketBinding();
                        UDP transport = super.createProtocol(stackConfiguration);
                        transport.setMulticasting(binding.getMulticastAddress() != null);
                        if (transport.supportsMulticasting()) {
                            transport.setMulticastAddress(binding.getMulticastAddress());
                            transport.setMulticastPort(binding.getMulticastPort());
                        }
                        return transport;
                    }
                };
            }
        }), entry.getValue());
    }
}
