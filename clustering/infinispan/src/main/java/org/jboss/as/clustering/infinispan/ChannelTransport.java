/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jboss.msc.value.Value;
import org.jgroups.Channel;

/**
 * Custom transport implementation that utilizes an injected channel.
 * @author Paul Ferraro
 */
public class ChannelTransport extends JGroupsTransport {

    private final Value<Channel> channelInjection;

    public ChannelTransport(Value<Channel> channelInjection) {
        this.channelInjection = channelInjection;
    }

    @Override
    protected void initChannel() {
        this.channel = this.channelInjection.getValue();
        this.startChannel = false;
        this.stopChannel = false;
        super.initChannel();
    }
}
