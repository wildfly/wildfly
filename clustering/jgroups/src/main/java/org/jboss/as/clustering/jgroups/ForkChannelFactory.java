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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.server.ServerEnvironment;
import org.jgroups.Channel;
import org.jgroups.fork.ForkChannel;
import org.jgroups.stack.Configurator;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;

/**
 * Factory for creating forked channels.
 * @author Paul Ferraro
 */
public class ForkChannelFactory implements ChannelFactory {

    final ProtocolStackConfiguration parentStack;
    final List<ProtocolConfiguration> protocols;
    final Channel channel;

    public ForkChannelFactory(Channel channel, ProtocolStackConfiguration parentStack, List<ProtocolConfiguration> protocols) {
        this.channel = channel;
        this.parentStack = parentStack;
        this.protocols = protocols;
    }

    public ForkChannelFactory(Channel channel, ProtocolStackConfiguration parentStack, ProtocolConfiguration... protocols) {
        this(channel, parentStack, Arrays.asList(protocols));
    }

    @Override
    public Channel createChannel(String id) throws Exception {
        JGroupsLogger.ROOT_LOGGER.debugf("Creating fork channel %s from channel %s", id, this.channel.getClusterName());

        final String stackName = this.protocols.isEmpty() ? this.channel.getClusterName() : id;
        ProtocolStackConfiguration forkStack = new ProtocolStackConfiguration() {
            @Override
            public String getName() {
                return stackName;
            }

            @Override
            public ProtocolDefaults getDefaults() {
                return ForkChannelFactory.this.parentStack.getDefaults();
            }

            @Override
            public TransportConfiguration getTransport() {
                return ForkChannelFactory.this.parentStack.getTransport();
            }

            @Override
            public List<ProtocolConfiguration> getProtocols() {
                return ForkChannelFactory.this.protocols;
            }

            @Override
            public ServerEnvironment getEnvironment() {
                return ForkChannelFactory.this.parentStack.getEnvironment();
            }

            @Override
            public RelayConfiguration getRelay() {
                return ForkChannelFactory.this.parentStack.getRelay();
            }
        };
        List<Protocol> protocols = Configurator.createProtocols(JChannelFactory.createProtocols(forkStack), new ProtocolStack());

        return new ForkChannel(this.channel, stackName, id, protocols.toArray(new Protocol[protocols.size()]));
    }

    @Override
    public ProtocolStackConfiguration getProtocolStackConfiguration() {
        return new ProtocolStackConfiguration() {
            @Override
            public String getName() {
                return ForkChannelFactory.this.channel.getClusterName();
            }

            @Override
            public ProtocolDefaults getDefaults() {
                return ForkChannelFactory.this.parentStack.getDefaults();
            }

            @Override
            public TransportConfiguration getTransport() {
                return ForkChannelFactory.this.parentStack.getTransport();
            }

            @Override
            public List<ProtocolConfiguration> getProtocols() {
                List<ProtocolConfiguration> parentProtocols = ForkChannelFactory.this.parentStack.getProtocols();
                List<ProtocolConfiguration> protocols = new ArrayList<>(parentProtocols.size() + ForkChannelFactory.this.protocols.size());
                protocols.addAll(parentProtocols);
                protocols.addAll(ForkChannelFactory.this.protocols);
                return protocols;
            }

            @Override
            public ServerEnvironment getEnvironment() {
                return ForkChannelFactory.this.parentStack.getEnvironment();
            }

            @Override
            public RelayConfiguration getRelay() {
                return ForkChannelFactory.this.parentStack.getRelay();
            }
        };
    }
}
