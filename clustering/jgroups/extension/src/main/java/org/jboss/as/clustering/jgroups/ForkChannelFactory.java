/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups;

import java.util.List;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jgroups.JChannel;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ForkStackConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;

/**
 * Factory for creating forked channels.
 * @author Paul Ferraro
 */
public class ForkChannelFactory implements org.wildfly.clustering.jgroups.spi.ForkChannelFactory {

    private final ForkStackConfiguration configuration;

    public ForkChannelFactory(ForkStackConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ForkStackConfiguration getForkStackConfiguration() {
        return this.configuration;
    }

    @Override
    public JChannel createChannel(String id) throws Exception {
        JChannel channel = this.configuration.getChannel();
        JGroupsLogger.ROOT_LOGGER.debugf("Creating fork channel %s from channel %s", id, channel.getClusterName());

        List<ProtocolConfiguration<? extends Protocol>> protocolConfigurations = this.configuration.getProtocols();
        String stackName = protocolConfigurations.isEmpty() ? channel.getClusterName() : id;

        ProtocolStackConfiguration stackConfiguration = this.configuration.getChannelFactory().getProtocolStackConfiguration();
        Protocol[] protocols = new Protocol[protocolConfigurations.size()];
        for (int i = 0; i < protocols.length; ++i) {
            protocols[i] = protocolConfigurations.get(i).createProtocol(stackConfiguration);
        }

        return new ForkChannel(channel, stackName, id, protocols);
    }

    static class ForkChannel extends org.jgroups.fork.ForkChannel {

        ForkChannel(JChannel channel, String stackName, String forkId, Protocol... protocols) throws Exception {
            super(channel, stackName, forkId, protocols);
        }

        @Override
        public ForkChannel setName(String name) {
            // No-op, super implementation logs error
            return this;
        }

        @Override
        public JChannel name(String name) {
            // No-op, super implementation logs error
            return this;
        }
    }
}
