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

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.function.Consumers;
import org.jboss.as.clustering.function.Functions;
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
 * Builds a non-clustered {@link org.wildfly.clustering.dispatcher.CommandDispatcherFactory} service.
 * @author Paul Ferraro
 */
public class LocalCommandDispatcherFactoryServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Supplier<AutoCloseableCommandDispatcherFactory> {

    private final String groupName;

    private volatile SupplierDependency<Group> group;

    public LocalCommandDispatcherFactoryServiceConfigurator(ServiceName name, String groupName) {
        super(name);
        this.groupName = groupName;
    }

    @Override
    public AutoCloseableCommandDispatcherFactory get() {
        return new ManagedCommandDispatcherFactory(new LocalCommandDispatcherFactory(this.group.get()));
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.group = new ServiceSupplierDependency<>(ClusteringRequirement.GROUP.getServiceName(support, this.groupName));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<CommandDispatcherFactory> factory = this.group.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(factory, Functions.identity(), this, Consumers.close());
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
