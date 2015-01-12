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

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.GroupServiceName;

/**
 * Builds a channel-based {@link Group} service.
 * @author Paul Ferraro
 */
public class ChannelGroupBuilder extends GroupServiceNameProvider implements Builder<Group>, Value<Group> {

    private final InjectedValue<CommandDispatcherFactory> factory = new InjectedValue<>();

    public ChannelGroupBuilder(String group) {
        super(group);
    }

    @Override
    public ServiceBuilder<Group> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(GroupServiceName.COMMAND_DISPATCHER.getServiceName(this.group), CommandDispatcherFactory.class, this.factory)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public Group getValue() {
        return this.factory.getValue().getGroup();
    }
}
