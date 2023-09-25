/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.web.service.routing.RouteLocatorServiceConfiguratorFactory;

/**
 * @author Paul Ferraro
 */
public abstract class AffinityServiceConfigurator<C> extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Supplier<RouteLocatorServiceConfiguratorFactory<C>> {

    public AffinityServiceConfigurator(Capability capability, PathAddress address) {
        super(capability, address);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<RouteLocatorServiceConfiguratorFactory<C>> factory = builder.provides(name);
        Service service = Service.newInstance(factory, this.get());
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
