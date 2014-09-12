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
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.spi.GroupServiceNames;

/**
 * Service that provides a channel instrumented {@link Group}.
 * @author Paul Ferraro
 */
public class ChannelGroupService implements Service<Group> {

    public static ServiceBuilder<Group> build(ServiceTarget target, ServiceName name, String group) {
        ChannelGroupService service = new ChannelGroupService();
        return target.addService(name, service)
                .addDependency(GroupServiceNames.COMMAND_DISPATCHER.getServiceName(group), CommandDispatcherFactory.class, service.factory)
        ;
    }

    private final InjectedValue<CommandDispatcherFactory> factory = new InjectedValue<>();
    private volatile Group group = null;

    private ChannelGroupService() {
        // Hide
    }

    @Override
    public Group getValue() {
        return this.group;
    }

    @Override
    public void start(StartContext context) {
        this.group = this.factory.getValue().getGroup();
    }

    @Override
    public void stop(StopContext context) {
        this.group = null;
    }
}
