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
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;
import org.jgroups.fork.ForkChannel;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;

/**
 * Service that creates a ForkChannel from a connected channel.
 *
 * @author Paul Ferraro
 */
public class ForkChannelService implements Service<Channel> {

    public static ServiceBuilder<Channel> build(ServiceTarget target, String channel, String parentChannel, Protocol... protocols) {
        ForkChannelService service = new ForkChannelService(channel, protocols);
        return AsynchronousService.addService(target, ChannelService.getServiceName(channel), service)
                .addDependency(ConnectedChannelService.getServiceName(parentChannel), Channel.class, service.channel)
        ;
    }

    private static final String EMPTY_STACK_ID = "empty";

    private final String id;
    private final Protocol[] protocols;

    private final InjectedValue<Channel> channel = new InjectedValue<>();

    private volatile Channel forkChannel;

    private ForkChannelService(String id, Protocol... protocols) {
        this.id = id;
        this.protocols = protocols;
    }

    @Override
    public Channel getValue() {
        return this.forkChannel;
    }

    @Override
    public void start(StartContext context) throws StartException {
        Channel channel = this.channel.getValue();
        try {
            JGroupsLogger.ROOT_LOGGER.debugf("Creating fork channel %s from channel %s", this.id, channel.getClusterName());
            String stackId = ((this.protocols != null) && (this.protocols.length > 0)) ? this.id : EMPTY_STACK_ID;
            Protocol top = channel.getProtocolStack().getTopProtocol();
            this.forkChannel = new ForkChannel(channel, stackId, this.id, true, ProtocolStack.ABOVE, top.getClass().asSubclass(Protocol.class), this.protocols);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        JGroupsLogger.ROOT_LOGGER.debugf("Closing fork channel %s", this.id);
        this.forkChannel.close();
        this.forkChannel = null;
    }
}
