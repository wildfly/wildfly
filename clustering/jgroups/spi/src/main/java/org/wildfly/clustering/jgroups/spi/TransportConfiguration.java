/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import java.util.Optional;

import org.jboss.as.network.SocketBinding;
import org.jgroups.protocols.TP;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Defines the configuration of a JGroups transport protocol.
 * @author Paul Ferraro
 */
public interface TransportConfiguration<T extends TP> extends ProtocolConfiguration<T> {
    @SuppressWarnings("unchecked")
    UnaryServiceDescriptor<TransportConfiguration<TP>> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.jgroups.transport", (Class<TransportConfiguration<TP>>) (Class<?>) TransportConfiguration.class);

    Topology getTopology();

    SocketBinding getSocketBinding();

    interface Topology {
        String getMachine();
        String getRack();
        String getSite();
    }

    default Optional<TLSConfiguration> getSSLConfiguration() {
        return Optional.empty();
    }

}
