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

import org.apache.activemq.artemis.api.core.JGroupsChannelBroadcastEndpoint;
import org.apache.activemq.artemis.api.core.jgroups.JChannelManager;
import org.apache.activemq.artemis.api.core.jgroups.JChannelWrapper;
import org.jgroups.JChannel;

/**
 * An implementation of {@link org.apache.activemq.artemis.api.core.JGroupsBroadcastEndpoint} which closes the channel
 * when no longer in use, e.g. to be used with FORK channels.
 *
 * @author Radoslav Husar
 */
public class ClosingChannelBroadcastEndpoint extends JGroupsChannelBroadcastEndpoint {

    public ClosingChannelBroadcastEndpoint(JChannelManager manager, JChannel jChannel, final String channelName) {
        super(manager, jChannel, channelName);
    }

    @Override
    protected synchronized void internalCloseChannel(JChannelWrapper channel) {
        channel.close(true);
    }

}
