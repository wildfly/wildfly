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

import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartException;
import org.jgroups.Channel;

/**
 * @author Paul Ferraro
 */
public class ChannelTransport extends JGroupsTransport {

    private final ServiceRegistry registry;
    private final ServiceName channelName;

    public ChannelTransport(ServiceRegistry registry, ServiceName channelName) {
        this.registry = registry;
        this.channelName = channelName;
    }

    @Override
    protected synchronized void initChannel() {
        ServiceController<Channel> service = ServiceContainerHelper.getService(this.registry, this.channelName);
        try {
            this.channel = ServiceContainerHelper.getValue(service);
            this.channel.setDiscardOwnMessages(false);
            this.connectChannel = true;
            this.disconnectChannel = true;
            this.closeChannel = false;
        } catch (StartException e) {
            throw new IllegalStateException(e);
        }
    }
}
