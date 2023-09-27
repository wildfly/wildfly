/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.network.SocketBindingManager;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.fork.ForkChannel;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;

/**
 * Factory for creating forked channels.
 * @author Paul Ferraro
 */
public class ForkChannelFactory implements ChannelFactory {

    private final ChannelFactory parentFactory;
    private final List<ProtocolConfiguration<? extends Protocol>> protocols;
    private final JChannel channel;

    public ForkChannelFactory(JChannel channel, ChannelFactory parentFactory, List<ProtocolConfiguration<? extends Protocol>> protocols) {
        this.channel = channel;
        this.parentFactory = parentFactory;
        this.protocols = protocols;
    }

    @Override
    public JChannel createChannel(String id) throws Exception {
        JGroupsLogger.ROOT_LOGGER.debugf("Creating fork channel %s from channel %s", id, this.channel.getClusterName());

        String stackName = this.protocols.isEmpty() ? this.channel.getClusterName() : id;

        Protocol[] protocols = new Protocol[this.protocols.size()];
        for (int i = 0; i < protocols.length; ++i) {
            protocols[i] = this.protocols.get(i).createProtocol(this.parentFactory.getProtocolStackConfiguration());
        }

        return new ForkChannel(this.channel, stackName, id, protocols);
    }

    @Override
    public ProtocolStackConfiguration getProtocolStackConfiguration() {
        ProtocolStackConfiguration parentStack = this.parentFactory.getProtocolStackConfiguration();
        return new ForkProtocolStackConfiguration(this.channel.getClusterName(), parentStack, Stream.concat(parentStack.getProtocols().stream(), this.protocols.stream()).collect(Collectors.toList()));
    }

    @Override
    public boolean isUnknownForkResponse(Message response) {
        return this.parentFactory.isUnknownForkResponse(response);
    }

    private static class ForkProtocolStackConfiguration implements ProtocolStackConfiguration {
        private final String name;
        private final List<ProtocolConfiguration<? extends Protocol>> protocols;
        private final ProtocolStackConfiguration parentStack;

        ForkProtocolStackConfiguration(String name, ProtocolStackConfiguration parentStack, List<ProtocolConfiguration<? extends Protocol>> protocols) {
            this.name = name;
            this.protocols = protocols;
            this.parentStack = parentStack;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public boolean isStatisticsEnabled() {
            return this.parentStack.isStatisticsEnabled();
        }

        @Override
        public List<ProtocolConfiguration<? extends Protocol>> getProtocols() {
            return this.protocols;
        }

        @Override
        public TransportConfiguration<? extends TP> getTransport() {
            return this.parentStack.getTransport();
        }

        @Override
        public String getNodeName() {
            return this.parentStack.getNodeName();
        }

        @Override
        public Optional<RelayConfiguration> getRelay() {
            return this.parentStack.getRelay();
        }

        @Override
        public SocketBindingManager getSocketBindingManager() {
            return this.parentStack.getSocketBindingManager();
        }
    }
}
