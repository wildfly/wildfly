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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.logging.JGroupsLogger.ROOT_LOGGER;

import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelListener;

/**
 * Provides a channel for use by dependent services.
 * Channels produced by this service are unconnected.
 * @author Paul Ferraro
 */
public class ChannelService implements Service<Channel>, ChannelListener {

    public static final String DEFAULT = "default";
    private static final String CHANNEL = "channel";
    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(JGroupsExtension.SUBSYSTEM_NAME).append(CHANNEL);

    public static ContextNames.BindInfo createChannelBinding(String channel) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, JGroupsExtension.SUBSYSTEM_NAME, CHANNEL, channel).getAbsoluteName());
    }

    public static ServiceName getServiceName(String id) {
        return SERVICE_NAME.append((id != null) ? id : DEFAULT);
    }

    public static ServiceName getFactoryServiceName(String channel) {
        return getServiceName(channel).append(ChannelFactoryService.FACTORY);
    }

    public static ServiceBuilder<Channel> build(ServiceTarget target, String id) {
        ChannelService service = new ChannelService(id);
        return target.addService(getServiceName(id), service)
                .addDependency(ChannelService.getFactoryServiceName(id), ChannelFactory.class, service.factory)
        ;
    }

    private final InjectedValue<ChannelFactory> factory = new InjectedValue<>();
    private final String id;

    private volatile Channel channel;

    private ChannelService(String id) {
        this.id = id;
    }

    @Override
    public Channel getValue() {
        return this.channel;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ChannelFactory factory = this.factory.getValue();
        try {
            this.channel = factory.createChannel(this.id);
            this.channel.addChannelListener(this);
        } catch (Exception e) {
            throw new StartException(e);
        }

        if (ROOT_LOGGER.isTraceEnabled())  {
            String output = this.channel.getProtocolStack().printProtocolSpec(true);
            ROOT_LOGGER.tracef("JGroups channel %s created with configuration:\n %s", this.id, output);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (this.channel != null) {
            this.channel.removeChannelListener(this);
            this.channel.close();
        }
        this.channel = null;
    }

    @Override
    public void channelClosed(Channel channel) {
        // Do nothing
    }

    @Override
    public void channelConnected(Channel channel) {
        // Validate view
        String localName = channel.getName();
        Address localAddress = channel.getAddress();
        for (Address address: channel.getView()) {
            String name = channel.getName(address);
            if ((name != null) && name.equals(localName) && !address.equals(localAddress)) {
                channel.close();
                throw JGroupsLogger.ROOT_LOGGER.duplicateNodeName(this.factory.getValue().getProtocolStackConfiguration().getEnvironment().getNodeName());
            }
        }
    }

    @Override
    public void channelDisconnected(Channel channel) {
        // Do nothing
    }
}
