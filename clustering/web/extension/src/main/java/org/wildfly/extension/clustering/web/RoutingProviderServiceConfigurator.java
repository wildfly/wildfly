/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.web.service.routing.RoutingProvider;

/**
 * Abstract service configurator for routing providers.
 * @author Paul Ferraro
 */
public abstract class RoutingProviderServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Supplier<RoutingProvider> {

    private final ServiceName alias;

    public RoutingProviderServiceConfigurator(PathAddress address) {
        this(address, null);
    }

    public RoutingProviderServiceConfigurator(PathAddress address, ServiceName alias) {
        super(RoutingProviderResourceDefinition.Capability.ROUTING_PROVIDER, address);
        this.alias = alias;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<RoutingProvider> provider = builder.provides((this.alias != null) ? new ServiceName[] { name, this.alias } : new ServiceName[] { name });
        Service service = new FunctionalService<>(provider, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
