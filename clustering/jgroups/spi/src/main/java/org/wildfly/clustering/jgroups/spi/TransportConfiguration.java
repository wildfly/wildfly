/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

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
        Topology UNSPECIFIED = new Topology() {
            @Override
            public Optional<String> getMachine() {
                return Optional.empty();
            }

            @Override
            public Optional<String> getRack() {
                return Optional.empty();
            }

            @Override
            public Optional<String> getSite() {
                return Optional.empty();
            }
        };

        Optional<String> getMachine();
        Optional<String> getRack();
        Optional<String> getSite();
    }

    default Optional<TLSConfiguration> getTLSConfiguration() {
        return Optional.empty();
    }

    @Override
    default boolean providesAuthentication() {
        return this.getTLSConfiguration().map(TLSConfiguration::getServerSSLContext).map(SSLContext::getDefaultSSLParameters).filter(SSLParameters::getNeedClientAuth).isPresent();
    }

    @Override
    default boolean providesConfidentiality() {
        return this.getTLSConfiguration().isPresent();
    }
}
