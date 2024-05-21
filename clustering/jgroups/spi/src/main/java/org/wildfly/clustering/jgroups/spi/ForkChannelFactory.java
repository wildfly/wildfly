/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.jgroups.spi;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.as.network.SocketBindingManager;
import org.jgroups.Message;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;

/**
 * @author Paul Ferraro
 */
public interface ForkChannelFactory extends ChannelFactory {

    ForkStackConfiguration getForkStackConfiguration();

    @Override
    default ProtocolStackConfiguration getProtocolStackConfiguration() {
        ForkStackConfiguration configuration = this.getForkStackConfiguration();
        ProtocolStackConfiguration parentStack = configuration.getChannelFactory().getProtocolStackConfiguration();
        List<ProtocolConfiguration<? extends Protocol>> protocols = Stream.concat(parentStack.getProtocols().stream(), configuration.getProtocols().stream()).collect(Collectors.toList());
        return new ProtocolStackConfiguration() {
            @Override
            public boolean isStatisticsEnabled() {
                return parentStack.isStatisticsEnabled();
            }

            @Override
            public TransportConfiguration<? extends TP> getTransport() {
                return parentStack.getTransport();
            }

            @Override
            public List<ProtocolConfiguration<? extends Protocol>> getProtocols() {
                return protocols;
            }

            @Override
            public String getMemberName() {
                return parentStack.getMemberName();
            }

            @Override
            public Optional<RelayConfiguration> getRelay() {
                return parentStack.getRelay();
            }

            @Override
            public SocketBindingManager getSocketBindingManager() {
                return parentStack.getSocketBindingManager();
            }
        };
    }

    @Override
    default boolean isUnknownForkResponse(Message response) {
        return this.getForkStackConfiguration().getChannelFactory().isUnknownForkResponse(response);
    }
}
