/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.dispatcher;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.function.Functions;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.infinispan.dispatcher.LocalCommandDispatcherFactory;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Builds a non-clustered {@link org.wildfly.clustering.dispatcher.CommandDispatcherFactory} service.
 * @author Paul Ferraro
 */
public class LocalCommandDispatcherFactoryServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Supplier<CommandDispatcherFactory> {

    private final String groupName;

    private volatile SupplierDependency<Group> group;

    public LocalCommandDispatcherFactoryServiceConfigurator(ServiceName name, String groupName) {
        super(name);
        this.groupName = groupName;
    }

    @Override
    public CommandDispatcherFactory get() {
        return new LocalCommandDispatcherFactory(this.group.get());
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
        Service service = new FunctionalService<>(factory, Functions.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
