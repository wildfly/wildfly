/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import java.util.Map;

import org.jboss.as.network.SocketBinding;
import org.jgroups.stack.Protocol;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;

/**
 * Defines the configuration of a JGroups protocol.
 * @author Paul Ferraro
 */
public interface ProtocolConfiguration<P extends Protocol> {
    @SuppressWarnings("unchecked")
    BinaryServiceDescriptor<ProtocolConfiguration<Protocol>> SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of("org.wildfly.clustering.jgroups.protocol", (Class<ProtocolConfiguration<Protocol>>) (Class<?>) ProtocolConfiguration.class);

    String getName();

    P createProtocol(ChannelFactoryConfiguration configuration);

    default Map<String, SocketBinding> getSocketBindings() {
        return Map.of();
    }
}
