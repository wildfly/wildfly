/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import org.apache.activemq.artemis.api.core.BroadcastEndpoint;
import org.apache.activemq.artemis.api.core.BroadcastEndpointFactory;
import org.apache.activemq.artemis.api.core.JGroupsBroadcastEndpoint;
import org.apache.activemq.artemis.api.core.jgroups.JChannelManager;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
@SuppressWarnings("serial")
public class JGroupsBroadcastEndpointFactory implements BroadcastEndpointFactory {
    private final ChannelFactory channelFactory;
    private final String channelName;

    /**
     * @param channels a Map of &lt;channel names, JChannel&gt; can will be filled with channels created from the broadcast endpoints
     */
    public JGroupsBroadcastEndpointFactory(ChannelFactory channelFactory, String channelName) {
        this.channelFactory = channelFactory;
        this.channelName = channelName;
    }

    @Override
    public BroadcastEndpoint createBroadcastEndpoint() throws Exception {
        JGroupsBroadcastEndpoint endpoint = new JGroupsBroadcastEndpoint(JChannelManager.getInstance(), channelName) {
            @Override
            public JChannel createChannel() throws Exception {
                return (JChannel) channelFactory.createChannel(channelName);
            }
        };
        return endpoint.initChannel();
    }
}
