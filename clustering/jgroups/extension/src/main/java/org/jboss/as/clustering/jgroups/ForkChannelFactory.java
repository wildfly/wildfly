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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.modules.ModuleLoader;
import org.jgroups.Channel;
import org.jgroups.fork.ForkChannel;
import org.jgroups.stack.Configurator;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
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
    private final List<ProtocolConfiguration> protocols;
    private final Channel channel;

    public ForkChannelFactory(Channel channel, ChannelFactory parentFactory, List<ProtocolConfiguration> protocols) {
        this.channel = channel;
        this.parentFactory = parentFactory;
        this.protocols = protocols;
    }

    public ForkChannelFactory(Channel channel, ChannelFactory parentFactory, ProtocolConfiguration... protocols) {
        this(channel, parentFactory, Arrays.asList(protocols));
    }

    @Override
    public Channel createChannel(String id) throws Exception {
        JGroupsLogger.ROOT_LOGGER.debugf("Creating fork channel %s from channel %s", id, this.channel.getClusterName());

        String stackName = this.protocols.isEmpty() ? this.channel.getClusterName() : id;
        ProtocolStackConfiguration forkStack = new ForkProtocolStackConfiguration(stackName, this.parentFactory.getProtocolStackConfiguration(), this.protocols);
        List<Protocol> protocols = Configurator.createProtocols(JChannelFactory.createProtocols(forkStack, this.channel.getProtocolStack().getTransport().isMulticastCapable()), new ProtocolStack());

        return new ForkChannel(this.channel, stackName, id, protocols.toArray(new Protocol[protocols.size()]));
    }

    @Override
    public ProtocolStackConfiguration getProtocolStackConfiguration() {
        List<ProtocolConfiguration> parentProtocols = this.parentFactory.getProtocolStackConfiguration().getProtocols();
        List<ProtocolConfiguration> protocols = new ArrayList<>(parentProtocols.size() + this.protocols.size());
        protocols.addAll(parentProtocols);
        protocols.addAll(this.protocols);
        return new ForkProtocolStackConfiguration(this.channel.getClusterName(), this.parentFactory.getProtocolStackConfiguration(), protocols);
    }

    @Override
    public boolean isUnknownForkResponse(ByteBuffer response) {
        return this.parentFactory.isUnknownForkResponse(response);
    }

    private static class ForkProtocolStackConfiguration implements ProtocolStackConfiguration {
        private final String name;
        private final List<ProtocolConfiguration> protocols;
        private final ProtocolStackConfiguration parentStack;

        ForkProtocolStackConfiguration(String name, ProtocolStackConfiguration parentStack, List<ProtocolConfiguration> protocols) {
            this.name = name;
            this.protocols = protocols;
            this.parentStack = parentStack;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public List<ProtocolConfiguration> getProtocols() {
            return this.protocols;
        }

        @Override
        public Map<String, String> getDefaultProperties(String protocol) {
            return this.parentStack.getDefaultProperties(protocol);
        }

        @Override
        public TransportConfiguration getTransport() {
            return this.parentStack.getTransport();
        }

        @Override
        public ModuleLoader getModuleLoader() {
            return this.parentStack.getModuleLoader();
        }

        @Override
        public String getNodeName() {
            return this.parentStack.getNodeName();
        }

        @Override
        public RelayConfiguration getRelay() {
            return this.parentStack.getRelay();
        }
    }
}
