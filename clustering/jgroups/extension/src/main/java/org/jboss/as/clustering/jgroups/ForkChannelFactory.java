/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.network.SocketBindingManager;
import org.jgroups.JChannel;
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
    public boolean isUnknownForkResponse(ByteBuffer response) {
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
