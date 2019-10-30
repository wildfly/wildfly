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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.spi.ClusteringRequirement;

/**
 * Builds a channel-based {@link Group} service.
 * @author Paul Ferraro
 */
public class ChannelGroupServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Supplier<Group> {

    private final String group;

    private volatile SupplierDependency<CommandDispatcherFactory> factory;

    public ChannelGroupServiceConfigurator(ServiceName name, String group) {
        super(name);
        this.group = group;
    }

    @Override
    public Group get() {
        return this.factory.get().getGroup();
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.factory = new ServiceSupplierDependency<>(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(support, this.group));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<Group> group = this.factory.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(group, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
