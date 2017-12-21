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
package org.wildfly.clustering.server.dispatcher;

import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.function.Consumers;
import org.jboss.as.clustering.function.Functions;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.SuppliedValueService;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.spi.ClusteringRequirement;

/**
 * Builds a non-clustered {@link org.wildfly.clustering.dispatcher.CommandDispatcherFactory} service.
 * @author Paul Ferraro
 */
public class LocalCommandDispatcherFactoryBuilder implements CapabilityServiceBuilder<CommandDispatcherFactory> {

    private final ServiceName name;
    private final String groupName;

    private volatile ValueDependency<Group> group;

    public LocalCommandDispatcherFactoryBuilder(ServiceName name, String groupName) {
        this.name = name;
        this.groupName = groupName;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public Builder<CommandDispatcherFactory> configure(CapabilityServiceSupport support) {
        this.group = new InjectedValueDependency<>(ClusteringRequirement.GROUP.getServiceName(support, this.groupName), Group.class);
        return this;
    }

    @Override
    public ServiceBuilder<CommandDispatcherFactory> build(ServiceTarget target) {
        Supplier<AutoCloseableCommandDispatcherFactory> supplier = () -> new ManagedCommandDispatcherFactory(new LocalCommandDispatcherFactory(this.group.getValue()));
        Service<CommandDispatcherFactory> service = new SuppliedValueService<>(Functions.identity(), supplier, Consumers.close());
        return this.group.register(target.addService(this.name, service).setInitialMode(ServiceController.Mode.ON_DEMAND));
    }
}
