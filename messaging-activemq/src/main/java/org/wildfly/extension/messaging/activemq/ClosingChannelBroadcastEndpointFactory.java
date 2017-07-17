/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
import org.apache.activemq.artemis.api.core.ChannelBroadcastEndpointFactory;
import org.apache.activemq.artemis.api.core.JGroupsBroadcastEndpoint;
import org.apache.activemq.artemis.api.core.JGroupsChannelBroadcastEndpoint;
import org.apache.activemq.artemis.api.core.jgroups.JChannelManager;
import org.apache.activemq.artemis.core.server.ActivateCallback;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jgroups.JChannel;
import org.jgroups.fork.ForkChannel;

/**
 * An implementation of {@link org.apache.activemq.artemis.api.core.BroadcastEndpointFactory} which closes the channel
 * when no longer in use, e.g. to be used with FORK channels.
 *
 * @author Radoslav Husar
 */
public class ClosingChannelBroadcastEndpointFactory extends ChannelBroadcastEndpointFactory {

    private final ActiveMQServer server;

    public ClosingChannelBroadcastEndpointFactory(JChannel channel, String channelName, ActiveMQServer server) {
        super(channel, channelName);
        this.server = server;
    }

    @Override
    public BroadcastEndpoint createBroadcastEndpoint() throws Exception {
        JGroupsBroadcastEndpoint endpoint;
        if (getChannel() instanceof ForkChannel) {
            // If the channel is a ForkChannel, close it only when ActiveMQ is deactivated
            endpoint = new JGroupsChannelBroadcastEndpoint(JChannelManager.getInstance(), getChannel(), getChannelName());
            server.registerActivateCallback(new ActivateCallback() {
                @Override
                public void preActivate() {

                }

                @Override
                public void activated() {

                }

                @Override
                public void deActivate() {
                    getChannel().close();
                }

                @Override
                public void activationComplete() {

                }
            });
        } else {
            // otherwise, always close the channel when internalCloseChannel is called.
            return new ClosingChannelBroadcastEndpoint(JChannelManager.getInstance(), this.getChannel(), this.getChannelName());
        }
        return endpoint.initChannel();
    }

}
