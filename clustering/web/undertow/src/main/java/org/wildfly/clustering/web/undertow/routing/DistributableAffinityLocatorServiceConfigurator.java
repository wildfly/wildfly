/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.routing;

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.web.session.AffinityLocator;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * Builds a distributable {@link DistributableAffinityLocator} service.
 *
 * @author Radoslav Husar
 */
public class DistributableAffinityLocatorServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator {

    private final SupplierDependency<RouteLocator> dependency;

    public DistributableAffinityLocatorServiceConfigurator(ServiceName name, SupplierDependency<RouteLocator> dependency) {
        super(name);
        this.dependency = dependency;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<AffinityLocator> affinityLocator = this.dependency.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(affinityLocator, DistributableAffinityLocator::new, this.dependency);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
