/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.group;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;
import org.wildfly.clustering.jgroups.spi.service.ChannelServiceName;
import org.wildfly.clustering.service.Builder;

/**
 * Builds a channel-based {@link NodeFactory} service.
 * @author Paul Ferraro
 */
public class ChannelNodeFactoryBuilder extends GroupNodeFactoryServiceNameProvider implements Builder<JGroupsNodeFactory>, Service<JGroupsNodeFactory> {

    private final InjectedValue<Channel> channel = new InjectedValue<>();

    private volatile ChannelNodeFactory factory;

    public ChannelNodeFactoryBuilder(String group) {
        super(group);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceBuilder<JGroupsNodeFactory> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), this)
                .addDependency(ChannelServiceName.CONNECTOR.getServiceName(this.group), Channel.class, this.channel)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public JGroupsNodeFactory getValue() {
        return this.factory;
    }

    @Override
    public void start(StartContext context) {
        this.factory = new ChannelNodeFactory(this.channel.getValue());
    }

    @Override
    public void stop(StopContext context) {
        this.factory.close();
        this.factory = null;
    }
}
