/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan;

import static org.jboss.as.clustering.infinispan.InfinispanMessages.MESSAGES;

import java.util.Properties;

import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.infinispan.remoting.transport.jgroups.JGroupsChannelLookup;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jboss.msc.value.Value;
import org.jgroups.Channel;

/**
 * @author Paul Ferraro
 */
public class ChannelProvider implements JGroupsChannelLookup {

    private static final String CHANNEL = "channel";

    public static void init(TransportConfigurationBuilder builder, Value<Channel> channel) {
        Properties properties = new Properties();
        properties.setProperty(JGroupsTransport.CHANNEL_LOOKUP, ChannelProvider.class.getName());
        properties.put(CHANNEL, channel);
        builder.transport().transport(new JGroupsTransport()).withProperties(properties);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.remoting.transport.jgroups.JGroupsChannelLookup#getJGroupsChannel(java.util.Properties)
     */
    @Override
    public Channel getJGroupsChannel(Properties properties) {
        @SuppressWarnings("unchecked")
        Value<Channel> channel = (Value<Channel>) properties.get(CHANNEL);

        if (channel == null) {
            throw MESSAGES.invalidTransportProperty(CHANNEL, properties);
        }
        return channel.getValue();
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.remoting.transport.jgroups.JGroupsChannelLookup#shouldStartAndConnect()
     */
    @Override
    public boolean shouldStartAndConnect() {
        return false;
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.remoting.transport.jgroups.JGroupsChannelLookup#shouldStopAndDisconnect()
     */
    @Override
    public boolean shouldStopAndDisconnect() {
        return false;
    }
}