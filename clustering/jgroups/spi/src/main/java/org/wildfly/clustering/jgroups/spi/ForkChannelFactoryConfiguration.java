/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import java.util.List;
import java.util.Optional;

import org.jboss.as.network.SocketBindingManager;
import org.jgroups.JChannel;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;

/**
 * @author Paul Ferraro
 *
 */
public interface ForkChannelFactoryConfiguration extends ChannelFactoryConfiguration {
    /**
     * The channel associate with this fork.
     * @return a JGroups channel
     */
    JChannel getChannel();

    /**
     * The configuration of the channel associate with this fork.
     * @return a JGroups channel configuration
     */
    ChannelConfiguration getChannelConfiguration();

    @Override
    default String getMemberName() {
        return this.getChannelConfiguration().getChannelFactory().getConfiguration().getMemberName();
    }

    @Override
    default boolean isStatisticsEnabled() {
        return this.getChannelConfiguration().getChannelFactory().getConfiguration().isStatisticsEnabled();
    }

    @Override
    default TransportConfiguration<? extends TP> getTransport() {
        return this.getChannelConfiguration().getChannelFactory().getConfiguration().getTransport();
    }

    @Override
    default List<ProtocolConfiguration<? extends Protocol>> getProtocols() {
        return List.of();
    }

    @Override
    default Optional<RelayConfiguration> getRelay() {
        return Optional.empty();
    }

    @Override
    default SocketBindingManager getSocketBindingManager() {
        return this.getChannelConfiguration().getChannelFactory().getConfiguration().getSocketBindingManager();
    }
}
